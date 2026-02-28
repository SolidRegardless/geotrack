-- V2: Relax position FK constraint
-- Positions arrive via Kafka from external sources (simulators, devices)
-- and may reference asset IDs not yet registered in the assets table.
-- Using a deterministic UUID (UUID v5 from asset string ID) allows
-- positions to be stored independently of asset registration.

ALTER TABLE positions DROP CONSTRAINT positions_asset_id_fkey;
