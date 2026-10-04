<?php

declare(strict_types=1);

final class V2StaffRepository implements V2StaffStore
{
    public function __construct(private PDO $pdo) {}

    public function listStaff(int $businessId): array
    {
        $stmt=$this->pdo->prepare(
            'SELECT external_id,name,title,phone,active,public_booking,version FROM staff
             WHERE business_id=:business ORDER BY name,id'
        );
        $stmt->execute(['business'=>$businessId]);
        return array_map([$this,'projectStaff'],$stmt->fetchAll() ?: []);
    }

    public function createStaff(int $businessId,array $input): array
    {
        $data=$this->validateStaff($input);
        $external=V2Id::external($input['external_id'] ?? null);
        $stmt=$this->pdo->prepare(
            'INSERT INTO staff(business_id,external_id,name,title,phone,active,public_booking)
             VALUES(:business,:external,:name,:title,:phone,:active,:public_booking)'
        );
        $stmt->execute([
            'business'=>$businessId,'external'=>$external,'name'=>$data['name'],'title'=>$data['title'],
            'phone'=>$data['phone'],'active'=>(int)$data['active'],'public_booking'=>(int)$data['public_booking'],
        ]);
        return $this->getStaff($businessId,$external);
    }

    public function updateStaff(int $businessId,string $externalId,array $input,int $expectedVersion): array
    {
        $current=$this->getStaff($businessId,$externalId);
        V2Version::guard($expectedVersion,(int)$current['version'],$current);
        $data=$this->validateStaff($input + $current);
        $stmt=$this->pdo->prepare(
            'UPDATE staff SET name=:name,title=:title,phone=:phone,active=:active,public_booking=:public_booking,version=version+1
             WHERE business_id=:business AND external_id=:external AND version=:expected'
        );
        $stmt->execute([
            'name'=>$data['name'],'title'=>$data['title'],'phone'=>$data['phone'],'active'=>(int)$data['active'],
            'public_booking'=>(int)$data['public_booking'],'business'=>$businessId,'external'=>$externalId,'expected'=>$expectedVersion,
        ]);
        if($stmt->rowCount()!==1) throw new V2VersionConflict($this->getStaff($businessId,$externalId));
        return $this->getStaff($businessId,$externalId);
    }

    public function deleteStaff(int $businessId,string $externalId,int $expectedVersion): void
    {
        $current=$this->getStaff($businessId,$externalId);
        V2Version::guard($expectedVersion,(int)$current['version'],$current);
        try {
            $stmt=$this->pdo->prepare(
                'DELETE FROM staff WHERE business_id=:business AND external_id=:external AND version=:expected'
            );
            $stmt->execute(['business'=>$businessId,'external'=>$externalId,'expected'=>$expectedVersion]);
        } catch(PDOException $e) {
            if((string)$e->getCode()==='23000') throw new RuntimeException('resource_in_use');
            throw $e;
        }
        if($stmt->rowCount()!==1) throw new RuntimeException(V2ApiError::NOT_FOUND);
    }

    public function listLeaves(int $businessId): array
    {
        $stmt=$this->pdo->prepare(
            'SELECT l.external_id,s.external_id AS staff_external_id,l.start_date,l.end_date,l.start_time,l.end_time,l.reason,l.version
             FROM staff_leaves l JOIN staff s ON s.id=l.staff_id AND s.business_id=l.business_id
             WHERE l.business_id=:business ORDER BY l.start_date,l.id'
        );
        $stmt->execute(['business'=>$businessId]);
        return array_map([$this,'projectLeave'],$stmt->fetchAll() ?: []);
    }

    public function createLeave(int $businessId,array $input): array
    {
        $data=$this->validateLeave($input);
        $staffId=$this->staffInternalId($businessId,$data['staff_external_id']);
        $external=V2Id::external($input['external_id'] ?? null);
        $stmt=$this->pdo->prepare(
            'INSERT INTO staff_leaves(business_id,external_id,staff_id,start_date,end_date,start_time,end_time,reason)
             VALUES(:business,:external,:staff,:start_date,:end_date,:start_time,:end_time,:reason)'
        );
        $stmt->execute([
            'business'=>$businessId,'external'=>$external,'staff'=>$staffId,'start_date'=>$data['start_date'],
            'end_date'=>$data['end_date'],'start_time'=>$data['start_time'],'end_time'=>$data['end_time'],'reason'=>$data['reason'],
        ]);
        return $this->getLeave($businessId,$external);
    }

    public function updateLeave(int $businessId,string $externalId,array $input,int $expectedVersion): array
    {
        $current=$this->getLeave($businessId,$externalId);
        V2Version::guard($expectedVersion,(int)$current['version'],$current);
        $data=$this->validateLeave($input + $current);
        $staffId=$this->staffInternalId($businessId,$data['staff_external_id']);
        $stmt=$this->pdo->prepare(
            'UPDATE staff_leaves SET staff_id=:staff,start_date=:start_date,end_date=:end_date,start_time=:start_time,
             end_time=:end_time,reason=:reason,version=version+1
             WHERE business_id=:business AND external_id=:external AND version=:expected'
        );
        $stmt->execute([
            'staff'=>$staffId,'start_date'=>$data['start_date'],'end_date'=>$data['end_date'],
            'start_time'=>$data['start_time'],'end_time'=>$data['end_time'],'reason'=>$data['reason'],
            'business'=>$businessId,'external'=>$externalId,'expected'=>$expectedVersion,
        ]);
        if($stmt->rowCount()!==1) throw new V2VersionConflict($this->getLeave($businessId,$externalId));
        return $this->getLeave($businessId,$externalId);
    }

    public function deleteLeave(int $businessId,string $externalId,int $expectedVersion): void
    {
        $current=$this->getLeave($businessId,$externalId);
        V2Version::guard($expectedVersion,(int)$current['version'],$current);
        $stmt=$this->pdo->prepare(
            'DELETE FROM staff_leaves WHERE business_id=:business AND external_id=:external AND version=:expected'
        );
        $stmt->execute(['business'=>$businessId,'external'=>$externalId,'expected'=>$expectedVersion]);
        if($stmt->rowCount()!==1) throw new RuntimeException(V2ApiError::NOT_FOUND);
    }

    private function getStaff(int $businessId,string $externalId): array
    {
        $stmt=$this->pdo->prepare(
            'SELECT external_id,name,title,phone,active,public_booking,version FROM staff
             WHERE business_id=:business AND external_id=:external LIMIT 1'
        );
        $stmt->execute(['business'=>$businessId,'external'=>$externalId]);
        $row=$stmt->fetch();
        if(!$row) throw new RuntimeException(V2ApiError::NOT_FOUND);
        return $this->projectStaff($row);
    }

    private function staffInternalId(int $businessId,string $externalId): int
    {
        $stmt=$this->pdo->prepare('SELECT id FROM staff WHERE business_id=:business AND external_id=:external LIMIT 1');
        $stmt->execute(['business'=>$businessId,'external'=>$externalId]);
        $id=$stmt->fetchColumn();
        if($id===false) throw new RuntimeException(V2ApiError::NOT_FOUND);
        return (int)$id;
    }

    private function getLeave(int $businessId,string $externalId): array
    {
        $stmt=$this->pdo->prepare(
            'SELECT l.external_id,s.external_id AS staff_external_id,l.start_date,l.end_date,l.start_time,l.end_time,l.reason,l.version
             FROM staff_leaves l JOIN staff s ON s.id=l.staff_id AND s.business_id=l.business_id
             WHERE l.business_id=:business AND l.external_id=:external LIMIT 1'
        );
        $stmt->execute(['business'=>$businessId,'external'=>$externalId]);
        $row=$stmt->fetch();
        if(!$row) throw new RuntimeException(V2ApiError::NOT_FOUND);
        return $this->projectLeave($row);
    }

    private function validateStaff(array $input): array
    {
        $name=trim((string)($input['name'] ?? ''));
        if($name==='' || strlen($name)>160) throw new RuntimeException(V2ApiError::VALIDATION_ERROR);
        $rawPhone=trim((string)($input['phone'] ?? ''));
        $phone=$rawPhone==='' ? null : Phone::normalize($rawPhone);
        if($rawPhone!=='' && $phone===null) throw new RuntimeException(V2ApiError::VALIDATION_ERROR);
        return [
            'name'=>$name,'title'=>trim((string)($input['title'] ?? '')),
            'phone'=>$phone,'active'=>(bool)($input['active'] ?? true),
            'public_booking'=>(bool)($input['public_booking'] ?? true),
        ];
    }

    private function validateLeave(array $input): array
    {
        $staffExternal=trim((string)($input['staff_external_id'] ?? ''));
        $start=trim((string)($input['start_date'] ?? ''));
        $end=trim((string)($input['end_date'] ?? ''));
        $startTime=trim((string)($input['start_time'] ?? '')) ?: null;
        $endTime=trim((string)($input['end_time'] ?? '')) ?: null;
        $startDate=DateTimeImmutable::createFromFormat('!Y-m-d',$start);
        $endDate=DateTimeImmutable::createFromFormat('!Y-m-d',$end);
        if($staffExternal==='' || !$startDate || !$endDate || $endDate<$startDate) {
            throw new RuntimeException(V2ApiError::VALIDATION_ERROR);
        }
        if(($startTime===null)!==($endTime===null)) throw new RuntimeException(V2ApiError::VALIDATION_ERROR);
        if($startTime!==null){
            $st=DateTimeImmutable::createFromFormat('!H:i',substr($startTime,0,5));
            $et=DateTimeImmutable::createFromFormat('!H:i',substr($endTime,0,5));
            if(!$st || !$et || $et<=$st) throw new RuntimeException(V2ApiError::VALIDATION_ERROR);
            $startTime=$st->format('H:i:s'); $endTime=$et->format('H:i:s');
        }
        return [
            'staff_external_id'=>$staffExternal,'start_date'=>$start,'end_date'=>$end,
            'start_time'=>$startTime,'end_time'=>$endTime,'reason'=>trim((string)($input['reason'] ?? '')),
        ];
    }

    private function projectStaff(array $row): array
    {
        return [
            'external_id'=>(string)$row['external_id'],'name'=>(string)$row['name'],
            'title'=>$row['title']===null?'':(string)$row['title'],'phone'=>$row['phone']===null?'':(string)$row['phone'],
            'active'=>(bool)$row['active'],'public_booking'=>(bool)$row['public_booking'],'version'=>(int)$row['version'],
        ];
    }

    private function projectLeave(array $row): array
    {
        return [
            'external_id'=>(string)$row['external_id'],'staff_external_id'=>(string)$row['staff_external_id'],
            'start_date'=>(string)$row['start_date'],'end_date'=>(string)$row['end_date'],
            'start_time'=>$row['start_time']===null?null:substr((string)$row['start_time'],0,5),
            'end_time'=>$row['end_time']===null?null:substr((string)$row['end_time'],0,5),
            'reason'=>$row['reason']===null?'':(string)$row['reason'],'version'=>(int)$row['version'],
        ];
    }
}
