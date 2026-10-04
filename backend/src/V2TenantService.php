<?php

declare(strict_types=1);

interface V2BusinessStore
{
    public function get(int $businessId): array;
    public function update(int $businessId, array $input, int $expectedVersion): array;
}

interface V2CatalogStore
{
    public function listServices(int $businessId): array;
    public function createService(int $businessId, array $input): array;
    public function updateService(int $businessId, string $externalId, array $input, int $expectedVersion): array;
    public function deleteService(int $businessId, string $externalId, int $expectedVersion): void;
}

interface V2StaffStore
{
    public function listStaff(int $businessId): array;
    public function createStaff(int $businessId, array $input): array;
    public function updateStaff(int $businessId, string $externalId, array $input, int $expectedVersion): array;
    public function deleteStaff(int $businessId, string $externalId, int $expectedVersion): void;

    public function listLeaves(int $businessId): array;
    public function createLeave(int $businessId, array $input): array;
    public function updateLeave(int $businessId, string $externalId, array $input, int $expectedVersion): array;
    public function deleteLeave(int $businessId, string $externalId, int $expectedVersion): void;
}

final class V2TenantService
{
    public function __construct(
        private V2BusinessStore $businesses,
        private V2CatalogStore $catalog,
        private V2StaffStore $staff
    ) {}

    private static function clean(array $input): array
    {
        unset($input['business_id']);
        return $input;
    }

    public function business(int $businessId): array
    {
        return $this->businesses->get($businessId);
    }

    public function updateBusiness(int $businessId, array $input, int $expectedVersion): array
    {
        return $this->businesses->update($businessId, self::clean($input), $expectedVersion);
    }

    public function listServices(int $businessId): array
    {
        return $this->catalog->listServices($businessId);
    }

    public function createService(int $businessId, array $input): array
    {
        return $this->catalog->createService($businessId, self::clean($input));
    }

    public function updateService(int $businessId, string $externalId, array $input, int $expectedVersion): array
    {
        return $this->catalog->updateService($businessId, $externalId, self::clean($input), $expectedVersion);
    }

    public function deleteService(int $businessId, string $externalId, int $expectedVersion): void
    {
        $this->catalog->deleteService($businessId, $externalId, $expectedVersion);
    }

    public function listStaff(int $businessId): array
    {
        return $this->staff->listStaff($businessId);
    }

    public function createStaff(int $businessId, array $input): array
    {
        return $this->staff->createStaff($businessId, self::clean($input));
    }

    public function updateStaff(int $businessId, string $externalId, array $input, int $expectedVersion): array
    {
        return $this->staff->updateStaff($businessId, $externalId, self::clean($input), $expectedVersion);
    }

    public function deleteStaff(int $businessId, string $externalId, int $expectedVersion): void
    {
        $this->staff->deleteStaff($businessId, $externalId, $expectedVersion);
    }

    public function listLeaves(int $businessId): array
    {
        return $this->staff->listLeaves($businessId);
    }

    public function createLeave(int $businessId, array $input): array
    {
        return $this->staff->createLeave($businessId, self::clean($input));
    }

    public function updateLeave(int $businessId, string $externalId, array $input, int $expectedVersion): array
    {
        return $this->staff->updateLeave($businessId, $externalId, self::clean($input), $expectedVersion);
    }

    public function deleteLeave(int $businessId, string $externalId, int $expectedVersion): void
    {
        $this->staff->deleteLeave($businessId, $externalId, $expectedVersion);
    }
}
