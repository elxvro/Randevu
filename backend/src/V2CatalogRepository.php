<?php

declare(strict_types=1);

final class V2CatalogRepository implements V2CatalogStore
{
    public function __construct(private PDO $pdo) {}

    public function listServices(int $businessId): array
    {
        $stmt = $this->pdo->prepare(
            'SELECT external_id,name,duration_minutes,price,active,version FROM services
             WHERE business_id=:business ORDER BY name,id'
        );
        $stmt->execute(['business'=>$businessId]);
        return array_map([$this,'project'], $stmt->fetchAll() ?: []);
    }

    public function createService(int $businessId, array $input): array
    {
        $data = $this->validate($input);
        $external = V2Id::external($input['external_id'] ?? null);
        $stmt = $this->pdo->prepare(
            'INSERT INTO services(business_id,external_id,name,duration_minutes,price,active)
             VALUES(:business,:external,:name,:duration,:price,:active)'
        );
        $stmt->execute([
            'business'=>$businessId,'external'=>$external,'name'=>$data['name'],
            'duration'=>$data['duration_minutes'],'price'=>$data['price'],'active'=>(int)$data['active'],
        ]);
        return $this->get($businessId,$external);
    }

    public function updateService(int $businessId, string $externalId, array $input, int $expectedVersion): array
    {
        $current = $this->get($businessId,$externalId);
        V2Version::guard($expectedVersion,(int)$current['version'],$current);
        $data = $this->validate($input + $current);
        $stmt = $this->pdo->prepare(
            'UPDATE services SET name=:name,duration_minutes=:duration,price=:price,active=:active,version=version+1
             WHERE business_id=:business AND external_id=:external AND version=:expected'
        );
        $stmt->execute([
            'name'=>$data['name'],'duration'=>$data['duration_minutes'],'price'=>$data['price'],
            'active'=>(int)$data['active'],'business'=>$businessId,'external'=>$externalId,'expected'=>$expectedVersion,
        ]);
        if ($stmt->rowCount() !== 1) throw new V2VersionConflict($this->get($businessId,$externalId));
        return $this->get($businessId,$externalId);
    }

    public function deleteService(int $businessId, string $externalId, int $expectedVersion): void
    {
        $current = $this->get($businessId,$externalId);
        V2Version::guard($expectedVersion,(int)$current['version'],$current);
        try {
            $stmt = $this->pdo->prepare(
                'DELETE FROM services WHERE business_id=:business AND external_id=:external AND version=:expected'
            );
            $stmt->execute(['business'=>$businessId,'external'=>$externalId,'expected'=>$expectedVersion]);
        } catch (PDOException $e) {
            if ((string)$e->getCode() === '23000') throw new RuntimeException('resource_in_use');
            throw $e;
        }
        if ($stmt->rowCount() !== 1) {
            $latest = $this->find($businessId,$externalId);
            if ($latest) throw new V2VersionConflict($latest);
            throw new RuntimeException(V2ApiError::NOT_FOUND);
        }
    }

    public function get(int $businessId,string $externalId): array
    {
        $row = $this->find($businessId,$externalId);
        if (!$row) throw new RuntimeException(V2ApiError::NOT_FOUND);
        return $row;
    }

    private function find(int $businessId,string $externalId): ?array
    {
        $stmt = $this->pdo->prepare(
            'SELECT external_id,name,duration_minutes,price,active,version FROM services
             WHERE business_id=:business AND external_id=:external LIMIT 1'
        );
        $stmt->execute(['business'=>$businessId,'external'=>$externalId]);
        $row=$stmt->fetch();
        return $row ? $this->project($row) : null;
    }

    private function validate(array $input): array
    {
        $name=trim((string)($input['name'] ?? ''));
        $duration=(int)($input['duration_minutes'] ?? 0);
        $price=(float)($input['price'] ?? 0);
        if ($name==='' || strlen($name)>160 || $duration<5 || $duration>1440 || $price<0) {
            throw new RuntimeException(V2ApiError::VALIDATION_ERROR);
        }
        return ['name'=>$name,'duration_minutes'=>$duration,'price'=>number_format($price,2,'.',''),'active'=>(bool)($input['active'] ?? true)];
    }

    private function project(array $row): array
    {
        return [
            'external_id'=>(string)$row['external_id'],
            'name'=>(string)$row['name'],
            'duration_minutes'=>(int)$row['duration_minutes'],
            'price'=>(string)$row['price'],
            'active'=>(bool)$row['active'],
            'version'=>(int)$row['version'],
        ];
    }
}
