<?php

declare(strict_types=1);

final class V2AppointmentRepository implements V2AppointmentStore
{
    public function __construct(private PDO $pdo) {}

    public function list(int $businessId, string $from, string $to): array
    {
        $business = $this->business($businessId);
        $zone = new DateTimeZone((string)$business['timezone']);
        $fromUtc = (new DateTimeImmutable($from . ' 00:00:00', $zone))->setTimezone(new DateTimeZone('UTC'))->format('Y-m-d H:i:s');
        $toUtc = (new DateTimeImmutable($to . ' 23:59:59', $zone))->setTimezone(new DateTimeZone('UTC'))->format('Y-m-d H:i:s');

        $stmt = $this->pdo->prepare(
            'SELECT a.*,sv.external_id AS service_external_id,sv.name AS service_name,
                    st.external_id AS staff_external_id,st.name AS staff_name,b.timezone
             FROM appointments a
             JOIN services sv ON sv.id=a.service_id AND sv.business_id=a.business_id
             JOIN staff st ON st.id=a.staff_id AND st.business_id=a.business_id
             JOIN businesses b ON b.id=a.business_id
             WHERE a.business_id=:business AND a.starts_at>=:from_utc AND a.starts_at<=:to_utc
             ORDER BY a.starts_at,a.id'
        );
        $stmt->execute(['business'=>$businessId,'from_utc'=>$fromUtc,'to_utc'=>$toUtc]);
        return array_map([$this,'project'], $stmt->fetchAll() ?: []);
    }

    public function create(int $businessId, array $payload, string $source): array
    {
        $external = V2Id::external($payload['external_id'] ?? null);
        $data = $this->resolveInput($businessId, $payload);

        $this->pdo->beginTransaction();
        try {
            $this->lockStaff($businessId, $data['staff_id']);
            $this->assertSlotAvailable($businessId, $data['staff_id'], $data['starts_at'], $data['ends_at'], null);
            $stmt = $this->pdo->prepare(
                'INSERT INTO appointments(
                    business_id,external_id,service_id,staff_id,customer_name,customer_phone,
                    starts_at,ends_at,status,source,note
                 ) VALUES(
                    :business,:external,:service,:staff,:customer_name,:customer_phone,
                    :starts_at,:ends_at,:status,:source,:note
                 )'
            );
            $stmt->execute([
                'business'=>$businessId,'external'=>$external,'service'=>$data['service_id'],'staff'=>$data['staff_id'],
                'customer_name'=>$data['customer_name'],'customer_phone'=>$data['customer_phone'],
                'starts_at'=>$data['starts_at'],'ends_at'=>$data['ends_at'],'status'=>$data['status'],
                'source'=>$source,'note'=>$data['note'],
            ]);
            $this->pdo->commit();
        } catch (Throwable $e) {
            if ($this->pdo->inTransaction()) $this->pdo->rollBack();
            throw $e;
        }
        return $this->get($businessId, $external);
    }

    public function update(int $businessId, string $externalId, array $payload, int $expectedVersion): array
    {
        $current = $this->get($businessId, $externalId);
        V2Version::guard($expectedVersion, (int)$current['version'], $current);
        $merged = [
            'service_external_id'=>$payload['service_external_id'] ?? $current['service_external_id'],
            'staff_external_id'=>$payload['staff_external_id'] ?? $current['staff_external_id'],
            'customer_name'=>$payload['customer_name'] ?? $current['customer_name'],
            'customer_phone'=>$payload['customer_phone'] ?? $current['customer_phone'],
            'date'=>$payload['date'] ?? $current['date'],
            'time'=>$payload['time'] ?? $current['time'],
            'status'=>$payload['status'] ?? $current['status'],
            'note'=>$payload['note'] ?? $current['note'],
        ];
        $data = $this->resolveInput($businessId, $merged);

        $this->pdo->beginTransaction();
        try {
            $this->lockStaff($businessId, $data['staff_id']);
            if (in_array($data['status'], ['pending','confirmed'], true)) {
                $this->assertSlotAvailable($businessId, $data['staff_id'], $data['starts_at'], $data['ends_at'], $externalId);
            }
            $stmt = $this->pdo->prepare(
                'UPDATE appointments SET service_id=:service,staff_id=:staff,customer_name=:customer_name,
                 customer_phone=:customer_phone,starts_at=:starts_at,ends_at=:ends_at,status=:status,
                 note=:note,version=version+1
                 WHERE business_id=:business AND external_id=:external AND version=:expected'
            );
            $stmt->execute([
                'service'=>$data['service_id'],'staff'=>$data['staff_id'],'customer_name'=>$data['customer_name'],
                'customer_phone'=>$data['customer_phone'],'starts_at'=>$data['starts_at'],'ends_at'=>$data['ends_at'],
                'status'=>$data['status'],'note'=>$data['note'],'business'=>$businessId,
                'external'=>$externalId,'expected'=>$expectedVersion,
            ]);
            if ($stmt->rowCount() !== 1) {
                $this->pdo->rollBack();
                throw new V2VersionConflict($this->get($businessId,$externalId));
            }
            $this->pdo->commit();
        } catch (Throwable $e) {
            if ($this->pdo->inTransaction()) $this->pdo->rollBack();
            throw $e;
        }
        return $this->get($businessId,$externalId);
    }

    public function cancel(int $businessId, string $externalId, int $expectedVersion): array
    {
        $current = $this->get($businessId,$externalId);
        V2Version::guard($expectedVersion,(int)$current['version'],$current);
        $stmt=$this->pdo->prepare(
            "UPDATE appointments SET status='cancelled',version=version+1
             WHERE business_id=:business AND external_id=:external AND version=:expected"
        );
        $stmt->execute(['business'=>$businessId,'external'=>$externalId,'expected'=>$expectedVersion]);
        if($stmt->rowCount()!==1) throw new V2VersionConflict($this->get($businessId,$externalId));
        return $this->get($businessId,$externalId);
    }

    public function get(int $businessId,string $externalId): array
    {
        $stmt=$this->pdo->prepare(
            'SELECT a.*,sv.external_id AS service_external_id,sv.name AS service_name,
                    st.external_id AS staff_external_id,st.name AS staff_name,b.timezone
             FROM appointments a
             JOIN services sv ON sv.id=a.service_id AND sv.business_id=a.business_id
             JOIN staff st ON st.id=a.staff_id AND st.business_id=a.business_id
             JOIN businesses b ON b.id=a.business_id
             WHERE a.business_id=:business AND a.external_id=:external LIMIT 1'
        );
        $stmt->execute(['business'=>$businessId,'external'=>$externalId]);
        $row=$stmt->fetch();
        if(!$row) throw new RuntimeException(V2ApiError::NOT_FOUND);
        return $this->project($row);
    }

    private function resolveInput(int $businessId,array $payload): array
    {
        $customerName=trim((string)($payload['customer_name'] ?? ''));
        $phone=Phone::normalize((string)($payload['customer_phone'] ?? ''));
        $serviceExternal=trim((string)($payload['service_external_id'] ?? $payload['service'] ?? ''));
        $staffExternal=trim((string)($payload['staff_external_id'] ?? $payload['staff'] ?? ''));
        $date=trim((string)($payload['date'] ?? ''));
        $time=trim((string)($payload['time'] ?? ''));
        $status=strtolower(trim((string)($payload['status'] ?? 'pending')));
        if($customerName==='' || strlen($customerName)>160 || $phone===null || !in_array($status,['pending','confirmed','completed','cancelled'],true)) {
            throw new RuntimeException(V2ApiError::VALIDATION_ERROR);
        }

        $serviceStmt=$this->pdo->prepare(
            'SELECT id,name,duration_minutes,active FROM services WHERE business_id=:business AND external_id=:external LIMIT 1'
        );
        $serviceStmt->execute(['business'=>$businessId,'external'=>$serviceExternal]);
        $service=$serviceStmt->fetch();
        if(!$service || !(bool)$service['active']) throw new RuntimeException(V2ApiError::NOT_FOUND);

        $staffStmt=$this->pdo->prepare(
            'SELECT id,name,active FROM staff WHERE business_id=:business AND external_id=:external LIMIT 1'
        );
        $staffStmt->execute(['business'=>$businessId,'external'=>$staffExternal]);
        $staff=$staffStmt->fetch();
        if(!$staff || !(bool)$staff['active']) throw new RuntimeException(V2ApiError::NOT_FOUND);

        $business=$this->business($businessId);
        try {
            $zone=new DateTimeZone((string)$business['timezone']);
            $local=new DateTimeImmutable($date.' '.$time,$zone);
        } catch(Throwable) {
            throw new RuntimeException(V2ApiError::VALIDATION_ERROR);
        }
        if($local->format('Y-m-d')!==$date || $local->format('H:i')!==substr($time,0,5)) {
            throw new RuntimeException(V2ApiError::VALIDATION_ERROR);
        }

        $minutes=((int)$local->format('H'))*60+(int)$local->format('i');
        $open=$this->timeMinutes((string)$business['opening_time']);
        $close=$this->timeMinutes((string)$business['closing_time']);
        $duration=(int)$service['duration_minutes'];
        if($open===null || $close===null || $minutes<$open || $minutes+$duration>$close) {
            throw new RuntimeException(V2ApiError::SLOT_UNAVAILABLE);
        }

        $leaveStmt=$this->pdo->prepare(
            'SELECT start_date,end_date,start_time,end_time FROM staff_leaves
             WHERE business_id=:business AND staff_id=:staff AND start_date<=:date AND end_date>=:date'
        );
        $leaveStmt->execute(['business'=>$businessId,'staff'=>(int)$staff['id'],'date'=>$date]);
        foreach($leaveStmt->fetchAll() ?: [] as $leave){
            $leaveStart=trim((string)($leave['start_time'] ?? ''));
            $leaveEnd=trim((string)($leave['end_time'] ?? ''));
            if($leaveStart==='' || $leaveEnd==='') throw new RuntimeException(V2ApiError::SLOT_UNAVAILABLE);
            $ls=$this->timeMinutes($leaveStart); $le=$this->timeMinutes($leaveEnd);
            if($ls!==null && $le!==null && $minutes<$le && $minutes+$duration>$ls) throw new RuntimeException(V2ApiError::SLOT_UNAVAILABLE);
        }

        $startsUtc=$local->setTimezone(new DateTimeZone('UTC'));
        $endsUtc=$local->modify("+{$duration} minutes")->setTimezone(new DateTimeZone('UTC'));
        return [
            'service_id'=>(int)$service['id'],'staff_id'=>(int)$staff['id'],
            'service_name'=>(string)$service['name'],'staff_name'=>(string)$staff['name'],
            'customer_name'=>$customerName,'customer_phone'=>$phone,'status'=>$status,
            'note'=>trim((string)($payload['note'] ?? '')),
            'starts_at'=>$startsUtc->format('Y-m-d H:i:s'),'ends_at'=>$endsUtc->format('Y-m-d H:i:s'),
        ];
    }

    private function business(int $businessId): array
    {
        $stmt=$this->pdo->prepare('SELECT timezone,opening_time,closing_time FROM businesses WHERE id=:business AND active=1 LIMIT 1');
        $stmt->execute(['business'=>$businessId]);
        $row=$stmt->fetch();
        if(!$row) throw new RuntimeException(V2ApiError::NOT_FOUND);
        return $row;
    }

    private function lockStaff(int $businessId,int $staffId): void
    {
        $stmt=$this->pdo->prepare(
            'SELECT id FROM staff WHERE id=:staff AND business_id=:business AND active=1 FOR UPDATE'
        );
        $stmt->execute(['staff'=>$staffId,'business'=>$businessId]);
        if($stmt->fetchColumn()===false) throw new RuntimeException(V2ApiError::NOT_FOUND);
    }

    private function assertSlotAvailable(int $businessId,int $staffId,string $starts,string $ends,?string $excludeExternal): void
    {
        $sql="SELECT external_id FROM appointments
              WHERE business_id=:business AND staff_id=:staff
                AND status IN ('pending','confirmed')
                AND starts_at < :ends AND ends_at > :starts";
        $params=['business'=>$businessId,'staff'=>$staffId,'starts'=>$starts,'ends'=>$ends];
        if($excludeExternal!==null){
            $sql.=' AND external_id<>:exclude_external';
            $params['exclude_external']=$excludeExternal;
        }
        $sql.=' LIMIT 1 FOR UPDATE';
        $stmt=$this->pdo->prepare($sql);
        $stmt->execute($params);
        if($stmt->fetchColumn()!==false) throw new RuntimeException(V2ApiError::SLOT_UNAVAILABLE);
    }

    private function project(array $row): array
    {
        $zone=new DateTimeZone((string)$row['timezone']);
        $local=(new DateTimeImmutable((string)$row['starts_at'],new DateTimeZone('UTC')))->setTimezone($zone);
        return [
            'external_id'=>(string)$row['external_id'],
            'service_external_id'=>(string)$row['service_external_id'],
            'staff_external_id'=>(string)$row['staff_external_id'],
            'customer_name'=>(string)$row['customer_name'],'customer_phone'=>(string)$row['customer_phone'],
            'date'=>$local->format('Y-m-d'),'time'=>$local->format('H:i'),
            'service'=>(string)$row['service_name'],'staff'=>(string)$row['staff_name'],
            'status'=>(string)$row['status'],'source'=>(string)$row['source'],
            'note'=>$row['note']===null?'':(string)$row['note'],'version'=>(int)$row['version'],
        ];
    }

    private function timeMinutes(string $value): ?int
    {
        if(!preg_match('/^(\d{2}):(\d{2})/',trim($value),$m)) return null;
        $h=(int)$m[1]; $min=(int)$m[2];
        return $h<=23 && $min<=59 ? $h*60+$min : null;
    }
}
