-- ============================================================
-- Seed data for Fuel Station & Price Comparison Application
-- Idempotent: uses INSERT ... ON CONFLICT DO NOTHING
-- ============================================================

-- ============================================================
-- Stations (35 stations across major French cities)
-- ============================================================

INSERT INTO stations (id, source_id, address, postal_code, city, latitude, longitude, presence_type, opening_hours, services, source, first_seen_at, last_seen_at, data_synchronized_at, active)
VALUES
  -- Paris (5 stations)
  (gen_random_uuid(), 'FR-00001', '15 Rue de Rivoli', '75001', 'Paris', 48.8566, 2.3488, 'station', 'Lun-Sam: 06:00-22:00, Dim: 08:00-20:00', 'Station services, Toilettes publiques', 'gouvernement_francais', now() - interval '30 days', now(), now(), true),
  (gen_random_uuid(), 'FR-00002', '42 Avenue des Champs-Elysees', '75008', 'Paris', 48.8698, 2.3075, 'station', 'Lun-Dim: 00:00-24:00', 'Station services, Toilettes publiques, Lavage auto', 'gouvernement_francais', now() - interval '30 days', now(), now(), true),
  (gen_random_uuid(), 'FR-00003', '8 Boulevard Haussmann', '75009', 'Paris', 48.8738, 2.3320, 'station', 'Lun-Sam: 06:30-22:30, Dim: 07:30-21:30', 'Station services, Borne electrique', 'gouvernement_francais', now() - interval '30 days', now(), now(), true),
  (gen_random_uuid(), 'FR-00004', '23 Rue de Belleville', '75020', 'Paris', 48.8650, 2.3910, 'station', 'Lun-Sam: 06:00-21:00, Dim: 08:00-20:00', 'Station services', 'gouvernement_francais', now() - interval '30 days', now(), now(), true),
  (gen_random_uuid(), 'FR-00005', '5 Place de la Nation', '75012', 'Paris', 48.8479, 2.3930, 'station', 'Lun-Sam: 07:00-22:00, Dim: 08:00-20:00', 'Station services, Toilettes publiques', 'gouvernement_francais', now() - interval '30 days', now(), now(), true),

  -- Lyon (5 stations)
  (gen_random_uuid(), 'FR-00006', '12 Rue de la Republique', '69002', 'Lyon', 45.7578, 4.8340, 'station', 'Lun-Sam: 06:00-22:00, Dim: 07:00-21:00', 'Station services, Toilettes publiques', 'gouvernement_francais', now() - interval '30 days', now(), now(), true),
  (gen_random_uuid(), 'FR-00007', '33 Avenue Jean Jaures', '69007', 'Lyon', 45.7490, 4.8450, 'station', 'Lun-Dim: 05:30-23:00', 'Station services, Lavage auto', 'gouvernement_francais', now() - interval '30 days', now(), now(), true),
  (gen_random_uuid(), 'FR-00008', '7 Quai Saint-Antoine', '69005', 'Lyon', 45.7520, 4.8290, 'station', 'Lun-Sam: 06:30-22:00, Dim: 08:00-20:00', 'Station services', 'gouvernement_francais', now() - interval '30 days', now(), now(), true),
  (gen_random_uuid(), 'FR-00009', '19 Rue Garibaldi', '69003', 'Lyon', 45.7530, 4.8540, 'station', 'Lun-Sam: 06:00-21:30, Dim: 08:00-19:30', 'Station services, Borne electrique', 'gouvernement_francais', now() - interval '30 days', now(), now(), true),
  (gen_random_uuid(), 'FR-00010', '45 Cours de la Liberation', '69008', 'Lyon', 45.7380, 4.8620, 'station', 'Lun-Sam: 07:00-22:00, Dim: 08:00-20:00', 'Station services, Toilettes publiques', 'gouvernement_francais', now() - interval '30 days', now(), now(), true),

  -- Marseille (4 stations)
  (gen_random_uuid(), 'FR-00011', '28 La Canebiere', '13001', 'Marseille', 43.2965, 5.3700, 'station', 'Lun-Sam: 06:00-22:00, Dim: 07:00-21:00', 'Station services, Toilettes publiques', 'gouvernement_francais', now() - interval '30 days', now(), now(), true),
  (gen_random_uuid(), 'FR-00012', '15 Boulevard Longchamp', '13003', 'Marseille', 43.3140, 5.3870, 'station', 'Lun-Sam: 06:30-22:30, Dim: 08:00-20:00', 'Station services', 'gouvernement_francais', now() - interval '30 days', now(), now(), true),
  (gen_random_uuid(), 'FR-00013', '9 Rue Sainte', '13002', 'Marseille', 43.2880, 5.3620, 'station', 'Lun-Dim: 00:00-24:00', 'Station services, Lavage auto, Toilettes publiques', 'gouvernement_francais', now() - interval '30 days', now(), now(), true),
  (gen_random_uuid(), 'FR-00014', '52 Avenue du Prado', '13006', 'Marseille', 43.2740, 5.3770, 'station', 'Lun-Sam: 06:00-21:00, Dim: 08:00-19:00', 'Station services, Borne electrique', 'gouvernement_francais', now() - interval '30 days', now(), now(), true),

  -- Toulouse (4 stations)
  (gen_random_uuid(), 'FR-00015', '31 Rue Jean Jaures', '31000', 'Toulouse', 43.6047, 1.4442, 'station', 'Lun-Sam: 06:00-22:00, Dim: 07:00-21:00', 'Station services, Toilettes publiques', 'gouvernement_francais', now() - interval '30 days', now(), now(), true),
  (gen_random_uuid(), 'FR-00016', '8 Allees Jean Jaures', '31000', 'Toulouse', 43.6100, 1.4490, 'station', 'Lun-Sam: 06:30-22:30, Dim: 08:00-20:00', 'Station services', 'gouvernement_francais', now() - interval '30 days', now(), now(), true),
  (gen_random_uuid(), 'FR-00017', '45 Boulevard de Strasbourg', '31000', 'Toulouse', 43.6080, 1.4530, 'station', 'Lun-Dim: 05:00-23:00', 'Station services, Lavage auto', 'gouvernement_francais', now() - interval '30 days', now(), now(), true),
  (gen_random_uuid(), 'FR-00018', '12 Rue Metz', '31000', 'Toulouse', 43.6020, 1.4410, 'station', 'Lun-Sam: 07:00-22:00, Dim: 08:00-20:00', 'Station services, Borne electrique', 'gouvernement_francais', now() - interval '30 days', now(), now(), true),

  -- Bordeaux (4 stations)
  (gen_random_uuid(), 'FR-00019', '16 Rue Sainte-Catherine', '33000', 'Bordeaux', 44.8378, -0.5792, 'station', 'Lun-Sam: 06:00-22:00, Dim: 07:00-21:00', 'Station services, Toilettes publiques', 'gouvernement_francais', now() - interval '30 days', now(), now(), true),
  (gen_random_uuid(), 'FR-00020', '29 Cours de l''Intendance', '33000', 'Bordeaux', 44.8435, -0.5730, 'station', 'Lun-Sam: 06:30-22:30, Dim: 08:00-20:00', 'Station services', 'gouvernement_francais', now() - interval '30 days', now(), now(), true),
  (gen_random_uuid(), 'FR-00021', '7 Quai des Chartrons', '33000', 'Bordeaux', 44.8420, -0.5690, 'station', 'Lun-Sam: 07:00-22:00, Dim: 08:00-20:00', 'Station services, Lavage auto', 'gouvernement_francais', now() - interval '30 days', now(), now(), true),
  (gen_random_uuid(), 'FR-00022', '53 Avenue Jean Jaures', '33000', 'Bordeaux', 44.8280, -0.5840, 'station', 'Lun-Dim: 06:00-23:00', 'Station services, Borne electrique, Toilettes publiques', 'gouvernement_francais', now() - interval '30 days', now(), now(), true),

  -- Strasbourg (3 stations)
  (gen_random_uuid(), 'FR-00023', '14 Rue des Halles', '67000', 'Strasbourg', 48.5810, 7.7507, 'station', 'Lun-Sam: 06:00-22:00, Dim: 08:00-20:00', 'Station services, Toilettes publiques', 'gouvernement_francais', now() - interval '30 days', now(), now(), true),
  (gen_random_uuid(), 'FR-00024', '27 Rue du Faubourg National', '67000', 'Strasbourg', 48.5780, 7.7430, 'station', 'Lun-Sam: 06:30-22:30, Dim: 08:00-20:00', 'Station services', 'gouvernement_francais', now() - interval '30 days', now(), now(), true),
  (gen_random_uuid(), 'FR-00025', '9 Place Kleber', '67000', 'Strasbourg', 48.5818, 7.7510, 'station', 'Lun-Sam: 07:00-22:00, Dim: 08:00-20:00', 'Station services, Lavage auto', 'gouvernement_francais', now() - interval '30 days', now(), now(), true),

  -- Nice (3 stations)
  (gen_random_uuid(), 'FR-00026', '22 Rue de France', '06000', 'Nice', 43.7102, 7.2620, 'station', 'Lun-Sam: 06:00-22:00, Dim: 07:00-21:00', 'Station services, Toilettes publiques', 'gouvernement_francais', now() - interval '30 days', now(), now(), true),
  (gen_random_uuid(), 'FR-00027', '11 Avenue Jean Medecin', '06000', 'Nice', 43.7105, 7.2680, 'station', 'Lun-Sam: 06:30-22:30, Dim: 08:00-20:00', 'Station services', 'gouvernement_francais', now() - interval '30 days', now(), now(), true),
  (gen_random_uuid(), 'FR-00028', '35 Promenade des Anglais', '06000', 'Nice', 43.6945, 7.2535, 'station', 'Lun-Dim: 00:00-24:00', 'Station services, Lavage auto, Borne electrique', 'gouvernement_francais', now() - interval '30 days', now(), now(), true),

  -- Lille (3 stations)
  (gen_random_uuid(), 'FR-00029', '18 Rue de la Grande Chaussee', '59000', 'Lille', 50.6366, 3.0635, 'station', 'Lun-Sam: 06:00-22:00, Dim: 08:00-20:00', 'Station services, Toilettes publiques', 'gouvernement_francais', now() - interval '30 days', now(), now(), true),
  (gen_random_uuid(), 'FR-00030', '7 Boulevard de la Liberte', '59000', 'Lille', 50.6290, 3.0580, 'station', 'Lun-Sam: 06:30-22:30, Dim: 08:00-20:00', 'Station services', 'gouvernement_francais', now() - interval '30 days', now(), now(), true),
  (gen_random_uuid(), 'FR-00031', '33 Rue Faidherbe', '59000', 'Lille', 50.6370, 3.0670, 'station', 'Lun-Sam: 07:00-22:00, Dim: 08:00-20:00', 'Station services, Borne electrique', 'gouvernement_francais', now() - interval '30 days', now(), now(), true),

  -- Nantes (3 stations)
  (gen_random_uuid(), 'FR-00032', '20 Rue de Strasbourg', '44000', 'Nantes', 47.2173, -1.5420, 'station', 'Lun-Sam: 06:00-22:00, Dim: 07:00-21:00', 'Station services, Toilettes publiques', 'gouvernement_francais', now() - interval '30 days', now(), now(), true),
  (gen_random_uuid(), 'FR-00033', '9 Place Royale', '44000', 'Nantes', 47.2150, -1.5530, 'station', 'Lun-Sam: 06:30-22:30, Dim: 08:00-20:00', 'Station services, Lavage auto', 'gouvernement_francais', now() - interval '30 days', now(), now(), true),
  (gen_random_uuid(), 'FR-00034', '41 Quai de la Fosse', '44000', 'Nantes', 47.2050, -1.5570, 'station', 'Lun-Sam: 07:00-22:00, Dim: 08:00-20:00', 'Station services', 'gouvernement_francais', now() - interval '30 days', now(), now(), true),

  -- Rennes (1 station)
  (gen_random_uuid(), 'FR-00035', '13 Rue Le Bastard', '35000', 'Rennes', 48.1113, -1.6800, 'station', 'Lun-Sam: 06:00-22:00, Dim: 08:00-20:00', 'Station services, Toilettes publiques, Borne electrique', 'gouvernement_francais', now() - interval '30 days', now(), now(), true)

ON CONFLICT (source_id, source) DO NOTHING;

-- ============================================================
-- Fuel Prices (2-4 per station, reported within last 48 hours)
-- ============================================================

DO $$
DECLARE
  sid UUID;
BEGIN
  SELECT id INTO sid FROM stations WHERE source_id = 'FR-00001' AND source = 'gouvernement_francais';
  INSERT INTO fuel_prices (id, station_id, fuel_type, price_per_liter, reported_at, availability, data_synchronized_at) VALUES
    (gen_random_uuid(), sid, 'Gazole', 1.759, now() - interval '6 hours', 'available', now()),
    (gen_random_uuid(), sid, 'E10',    1.829, now() - interval '6 hours', 'available', now()),
    (gen_random_uuid(), sid, 'SP98',   1.959, now() - interval '6 hours', 'available', now())
  ON CONFLICT (station_id, fuel_type) DO NOTHING;

  SELECT id INTO sid FROM stations WHERE source_id = 'FR-00002' AND source = 'gouvernement_francais';
  INSERT INTO fuel_prices (id, station_id, fuel_type, price_per_liter, reported_at, availability, data_synchronized_at) VALUES
    (gen_random_uuid(), sid, 'Gazole', 1.789, now() - interval '12 hours', 'available', now()),
    (gen_random_uuid(), sid, 'SP95',   1.899, now() - interval '12 hours', 'available', now()),
    (gen_random_uuid(), sid, 'E10',    1.849, now() - interval '12 hours', 'available', now()),
    (gen_random_uuid(), sid, 'SP98',   1.989, now() - interval '12 hours', 'available', now())
  ON CONFLICT (station_id, fuel_type) DO NOTHING;

  SELECT id INTO sid FROM stations WHERE source_id = 'FR-00003' AND source = 'gouvernement_francais';
  INSERT INTO fuel_prices (id, station_id, fuel_type, price_per_liter, reported_at, availability, data_synchronized_at) VALUES
    (gen_random_uuid(), sid, 'Gazole', 1.749, now() - interval '3 hours', 'available', now()),
    (gen_random_uuid(), sid, 'E10',    1.819, now() - interval '3 hours', 'available', now()),
    (gen_random_uuid(), sid, 'GPLc',   0.929, now() - interval '3 hours', 'available', now())
  ON CONFLICT (station_id, fuel_type) DO NOTHING;

  SELECT id INTO sid FROM stations WHERE source_id = 'FR-00004' AND source = 'gouvernement_francais';
  INSERT INTO fuel_prices (id, station_id, fuel_type, price_per_liter, reported_at, availability, data_synchronized_at) VALUES
    (gen_random_uuid(), sid, 'Gazole', 1.769, now() - interval '18 hours', 'available', now()),
    (gen_random_uuid(), sid, 'E85',    0.989, now() - interval '18 hours', 'available', now())
  ON CONFLICT (station_id, fuel_type) DO NOTHING;

  SELECT id INTO sid FROM stations WHERE source_id = 'FR-00005' AND source = 'gouvernement_francais';
  INSERT INTO fuel_prices (id, station_id, fuel_type, price_per_liter, reported_at, availability, data_synchronized_at) VALUES
    (gen_random_uuid(), sid, 'Gazole', 1.779, now() - interval '10 hours', 'available', now()),
    (gen_random_uuid(), sid, 'SP95',   1.879, now() - interval '10 hours', 'available', now()),
    (gen_random_uuid(), sid, 'E10',    1.839, now() - interval '10 hours', 'available', now())
  ON CONFLICT (station_id, fuel_type) DO NOTHING;

  SELECT id INTO sid FROM stations WHERE source_id = 'FR-00006' AND source = 'gouvernement_francais';
  INSERT INTO fuel_prices (id, station_id, fuel_type, price_per_liter, reported_at, availability, data_synchronized_at) VALUES
    (gen_random_uuid(), sid, 'Gazole', 1.739, now() - interval '5 hours', 'available', now()),
    (gen_random_uuid(), sid, 'E10',    1.809, now() - interval '5 hours', 'available', now()),
    (gen_random_uuid(), sid, 'SP98',   1.949, now() - interval '5 hours', 'available', now())
  ON CONFLICT (station_id, fuel_type) DO NOTHING;

  SELECT id INTO sid FROM stations WHERE source_id = 'FR-00007' AND source = 'gouvernement_francais';
  INSERT INTO fuel_prices (id, station_id, fuel_type, price_per_liter, reported_at, availability, data_synchronized_at) VALUES
    (gen_random_uuid(), sid, 'Gazole', 1.759, now() - interval '15 hours', 'available', now()),
    (gen_random_uuid(), sid, 'SP95',   1.869, now() - interval '15 hours', 'available', now()),
    (gen_random_uuid(), sid, 'E85',    0.969, now() - interval '15 hours', 'available', now()),
    (gen_random_uuid(), sid, 'GPLc',   0.899, now() - interval '15 hours', 'available', now())
  ON CONFLICT (station_id, fuel_type) DO NOTHING;

  SELECT id INTO sid FROM stations WHERE source_id = 'FR-00008' AND source = 'gouvernement_francais';
  INSERT INTO fuel_prices (id, station_id, fuel_type, price_per_liter, reported_at, availability, data_synchronized_at) VALUES
    (gen_random_uuid(), sid, 'Gazole', 1.729, now() - interval '8 hours', 'available', now()),
    (gen_random_uuid(), sid, 'E10',    1.799, now() - interval '8 hours', 'available', now())
  ON CONFLICT (station_id, fuel_type) DO NOTHING;

  SELECT id INTO sid FROM stations WHERE source_id = 'FR-00009' AND source = 'gouvernement_francais';
  INSERT INTO fuel_prices (id, station_id, fuel_type, price_per_liter, reported_at, availability, data_synchronized_at) VALUES
    (gen_random_uuid(), sid, 'Gazole', 1.769, now() - interval '20 hours', 'available', now()),
    (gen_random_uuid(), sid, 'SP95',   1.889, now() - interval '20 hours', 'available', now()),
    (gen_random_uuid(), sid, 'E10',    1.819, now() - interval '20 hours', 'available', now()),
    (gen_random_uuid(), sid, 'SP98',   1.979, now() - interval '20 hours', 'available', now())
  ON CONFLICT (station_id, fuel_type) DO NOTHING;

  SELECT id INTO sid FROM stations WHERE source_id = 'FR-00010' AND source = 'gouvernement_francais';
  INSERT INTO fuel_prices (id, station_id, fuel_type, price_per_liter, reported_at, availability, data_synchronized_at) VALUES
    (gen_random_uuid(), sid, 'Gazole', 1.749, now() - interval '2 hours', 'available', now()),
    (gen_random_uuid(), sid, 'GPLc',   0.919, now() - interval '2 hours', 'available', now())
  ON CONFLICT (station_id, fuel_type) DO NOTHING;

  SELECT id INTO sid FROM stations WHERE source_id = 'FR-00011' AND source = 'gouvernement_francais';
  INSERT INTO fuel_prices (id, station_id, fuel_type, price_per_liter, reported_at, availability, data_synchronized_at) VALUES
    (gen_random_uuid(), sid, 'Gazole', 1.719, now() - interval '7 hours', 'available', now()),
    (gen_random_uuid(), sid, 'E10',    1.789, now() - interval '7 hours', 'available', now()),
    (gen_random_uuid(), sid, 'SP98',   1.939, now() - interval '7 hours', 'available', now())
  ON CONFLICT (station_id, fuel_type) DO NOTHING;

  SELECT id INTO sid FROM stations WHERE source_id = 'FR-00012' AND source = 'gouvernement_francais';
  INSERT INTO fuel_prices (id, station_id, fuel_type, price_per_liter, reported_at, availability, data_synchronized_at) VALUES
    (gen_random_uuid(), sid, 'Gazole', 1.709, now() - interval '14 hours', 'available', now()),
    (gen_random_uuid(), sid, 'SP95',   1.859, now() - interval '14 hours', 'available', now())
  ON CONFLICT (station_id, fuel_type) DO NOTHING;

  SELECT id INTO sid FROM stations WHERE source_id = 'FR-00013' AND source = 'gouvernement_francais';
  INSERT INTO fuel_prices (id, station_id, fuel_type, price_per_liter, reported_at, availability, data_synchronized_at) VALUES
    (gen_random_uuid(), sid, 'Gazole', 1.699, now() - interval '1 hours', 'available', now()),
    (gen_random_uuid(), sid, 'E10',    1.779, now() - interval '1 hours', 'available', now()),
    (gen_random_uuid(), sid, 'E85',    0.949, now() - interval '1 hours', 'available', now()),
    (gen_random_uuid(), sid, 'GPLc',   0.879, now() - interval '1 hours', 'available', now())
  ON CONFLICT (station_id, fuel_type) DO NOTHING;

  SELECT id INTO sid FROM stations WHERE source_id = 'FR-00014' AND source = 'gouvernement_francais';
  INSERT INTO fuel_prices (id, station_id, fuel_type, price_per_liter, reported_at, availability, data_synchronized_at) VALUES
    (gen_random_uuid(), sid, 'Gazole', 1.729, now() - interval '22 hours', 'available', now()),
    (gen_random_uuid(), sid, 'E10',    1.799, now() - interval '22 hours', 'available', now())
  ON CONFLICT (station_id, fuel_type) DO NOTHING;

  SELECT id INTO sid FROM stations WHERE source_id = 'FR-00015' AND source = 'gouvernement_francais';
  INSERT INTO fuel_prices (id, station_id, fuel_type, price_per_liter, reported_at, availability, data_synchronized_at) VALUES
    (gen_random_uuid(), sid, 'Gazole', 1.689, now() - interval '4 hours', 'available', now()),
    (gen_random_uuid(), sid, 'E10',    1.769, now() - interval '4 hours', 'available', now()),
    (gen_random_uuid(), sid, 'SP98',   1.929, now() - interval '4 hours', 'available', now())
  ON CONFLICT (station_id, fuel_type) DO NOTHING;

  SELECT id INTO sid FROM stations WHERE source_id = 'FR-00016' AND source = 'gouvernement_francais';
  INSERT INTO fuel_prices (id, station_id, fuel_type, price_per_liter, reported_at, availability, data_synchronized_at) VALUES
    (gen_random_uuid(), sid, 'Gazole', 1.709, now() - interval '11 hours', 'available', now()),
    (gen_random_uuid(), sid, 'SP95',   1.849, now() - interval '11 hours', 'available', now()),
    (gen_random_uuid(), sid, 'E85',    0.939, now() - interval '11 hours', 'available', now())
  ON CONFLICT (station_id, fuel_type) DO NOTHING;

  SELECT id INTO sid FROM stations WHERE source_id = 'FR-00017' AND source = 'gouvernement_francais';
  INSERT INTO fuel_prices (id, station_id, fuel_type, price_per_liter, reported_at, availability, data_synchronized_at) VALUES
    (gen_random_uuid(), sid, 'Gazole', 1.699, now() - interval '16 hours', 'available', now()),
    (gen_random_uuid(), sid, 'E10',    1.779, now() - interval '16 hours', 'available', now())
  ON CONFLICT (station_id, fuel_type) DO NOTHING;

  SELECT id INTO sid FROM stations WHERE source_id = 'FR-00018' AND source = 'gouvernement_francais';
  INSERT INTO fuel_prices (id, station_id, fuel_type, price_per_liter, reported_at, availability, data_synchronized_at) VALUES
    (gen_random_uuid(), sid, 'Gazole', 1.719, now() - interval '9 hours', 'available', now()),
    (gen_random_uuid(), sid, 'GPLc',   0.889, now() - interval '9 hours', 'available', now())
  ON CONFLICT (station_id, fuel_type) DO NOTHING;

  SELECT id INTO sid FROM stations WHERE source_id = 'FR-00019' AND source = 'gouvernement_francais';
  INSERT INTO fuel_prices (id, station_id, fuel_type, price_per_liter, reported_at, availability, data_synchronized_at) VALUES
    (gen_random_uuid(), sid, 'Gazole', 1.729, now() - interval '6 hours', 'available', now()),
    (gen_random_uuid(), sid, 'E10',    1.799, now() - interval '6 hours', 'available', now()),
    (gen_random_uuid(), sid, 'SP98',   1.959, now() - interval '6 hours', 'available', now())
  ON CONFLICT (station_id, fuel_type) DO NOTHING;

  SELECT id INTO sid FROM stations WHERE source_id = 'FR-00020' AND source = 'gouvernement_francais';
  INSERT INTO fuel_prices (id, station_id, fuel_type, price_per_liter, reported_at, availability, data_synchronized_at) VALUES
    (gen_random_uuid(), sid, 'Gazole', 1.749, now() - interval '13 hours', 'available', now()),
    (gen_random_uuid(), sid, 'SP95',   1.869, now() - interval '13 hours', 'available', now())
  ON CONFLICT (station_id, fuel_type) DO NOTHING;

  SELECT id INTO sid FROM stations WHERE source_id = 'FR-00021' AND source = 'gouvernement_francais';
  INSERT INTO fuel_prices (id, station_id, fuel_type, price_per_liter, reported_at, availability, data_synchronized_at) VALUES
    (gen_random_uuid(), sid, 'Gazole', 1.739, now() - interval '19 hours', 'available', now()),
    (gen_random_uuid(), sid, 'E10',    1.809, now() - interval '19 hours', 'available', now()),
    (gen_random_uuid(), sid, 'E85',    0.959, now() - interval '19 hours', 'available', now())
  ON CONFLICT (station_id, fuel_type) DO NOTHING;

  SELECT id INTO sid FROM stations WHERE source_id = 'FR-00022' AND source = 'gouvernement_francais';
  INSERT INTO fuel_prices (id, station_id, fuel_type, price_per_liter, reported_at, availability, data_synchronized_at) VALUES
    (gen_random_uuid(), sid, 'Gazole', 1.759, now() - interval '25 hours', 'available', now()),
    (gen_random_uuid(), sid, 'E10',    1.819, now() - interval '25 hours', 'available', now()),
    (gen_random_uuid(), sid, 'SP98',   1.969, now() - interval '25 hours', 'available', now()),
    (gen_random_uuid(), sid, 'GPLc',   0.909, now() - interval '25 hours', 'available', now())
  ON CONFLICT (station_id, fuel_type) DO NOTHING;

  SELECT id INTO sid FROM stations WHERE source_id = 'FR-00023' AND source = 'gouvernement_francais';
  INSERT INTO fuel_prices (id, station_id, fuel_type, price_per_liter, reported_at, availability, data_synchronized_at) VALUES
    (gen_random_uuid(), sid, 'Gazole', 1.709, now() - interval '4 hours', 'available', now()),
    (gen_random_uuid(), sid, 'E10',    1.789, now() - interval '4 hours', 'available', now())
  ON CONFLICT (station_id, fuel_type) DO NOTHING;

  SELECT id INTO sid FROM stations WHERE source_id = 'FR-00024' AND source = 'gouvernement_francais';
  INSERT INTO fuel_prices (id, station_id, fuel_type, price_per_liter, reported_at, availability, data_synchronized_at) VALUES
    (gen_random_uuid(), sid, 'Gazole', 1.699, now() - interval '17 hours', 'available', now()),
    (gen_random_uuid(), sid, 'SP95',   1.839, now() - interval '17 hours', 'available', now()),
    (gen_random_uuid(), sid, 'SP98',   1.919, now() - interval '17 hours', 'available', now())
  ON CONFLICT (station_id, fuel_type) DO NOTHING;

  SELECT id INTO sid FROM stations WHERE source_id = 'FR-00025' AND source = 'gouvernement_francais';
  INSERT INTO fuel_prices (id, station_id, fuel_type, price_per_liter, reported_at, availability, data_synchronized_at) VALUES
    (gen_random_uuid(), sid, 'Gazole', 1.719, now() - interval '10 hours', 'available', now()),
    (gen_random_uuid(), sid, 'E10',    1.799, now() - interval '10 hours', 'available', now()),
    (gen_random_uuid(), sid, 'E85',    0.929, now() - interval '10 hours', 'available', now())
  ON CONFLICT (station_id, fuel_type) DO NOTHING;

  SELECT id INTO sid FROM stations WHERE source_id = 'FR-00026' AND source = 'gouvernement_francais';
  INSERT INTO fuel_prices (id, station_id, fuel_type, price_per_liter, reported_at, availability, data_synchronized_at) VALUES
    (gen_random_uuid(), sid, 'Gazole', 1.779, now() - interval '8 hours', 'available', now()),
    (gen_random_uuid(), sid, 'E10',    1.849, now() - interval '8 hours', 'available', now()),
    (gen_random_uuid(), sid, 'SP98',   1.999, now() - interval '8 hours', 'available', now())
  ON CONFLICT (station_id, fuel_type) DO NOTHING;

  SELECT id INTO sid FROM stations WHERE source_id = 'FR-00027' AND source = 'gouvernement_francais';
  INSERT INTO fuel_prices (id, station_id, fuel_type, price_per_liter, reported_at, availability, data_synchronized_at) VALUES
    (gen_random_uuid(), sid, 'Gazole', 1.789, now() - interval '21 hours', 'available', now()),
    (gen_random_uuid(), sid, 'SP95',   1.899, now() - interval '21 hours', 'available', now())
  ON CONFLICT (station_id, fuel_type) DO NOTHING;

  SELECT id INTO sid FROM stations WHERE source_id = 'FR-00028' AND source = 'gouvernement_francais';
  INSERT INTO fuel_prices (id, station_id, fuel_type, price_per_liter, reported_at, availability, data_synchronized_at) VALUES
    (gen_random_uuid(), sid, 'Gazole', 1.769, now() - interval '1 hours', 'available', now()),
    (gen_random_uuid(), sid, 'E10',    1.839, now() - interval '1 hours', 'available', now()),
    (gen_random_uuid(), sid, 'E85',    0.999, now() - interval '1 hours', 'available', now()),
    (gen_random_uuid(), sid, 'GPLc',   0.939, now() - interval '1 hours', 'available', now())
  ON CONFLICT (station_id, fuel_type) DO NOTHING;

  SELECT id INTO sid FROM stations WHERE source_id = 'FR-00029' AND source = 'gouvernement_francais';
  INSERT INTO fuel_prices (id, station_id, fuel_type, price_per_liter, reported_at, availability, data_synchronized_at) VALUES
    (gen_random_uuid(), sid, 'Gazole', 1.699, now() - interval '5 hours', 'available', now()),
    (gen_random_uuid(), sid, 'E10',    1.779, now() - interval '5 hours', 'available', now())
  ON CONFLICT (station_id, fuel_type) DO NOTHING;

  SELECT id INTO sid FROM stations WHERE source_id = 'FR-00030' AND source = 'gouvernement_francais';
  INSERT INTO fuel_prices (id, station_id, fuel_type, price_per_liter, reported_at, availability, data_synchronized_at) VALUES
    (gen_random_uuid(), sid, 'Gazole', 1.689, now() - interval '14 hours', 'available', now()),
    (gen_random_uuid(), sid, 'SP95',   1.829, now() - interval '14 hours', 'available', now()),
    (gen_random_uuid(), sid, 'SP98',   1.909, now() - interval '14 hours', 'available', now())
  ON CONFLICT (station_id, fuel_type) DO NOTHING;

  SELECT id INTO sid FROM stations WHERE source_id = 'FR-00031' AND source = 'gouvernement_francais';
  INSERT INTO fuel_prices (id, station_id, fuel_type, price_per_liter, reported_at, availability, data_synchronized_at) VALUES
    (gen_random_uuid(), sid, 'Gazole', 1.709, now() - interval '23 hours', 'available', now()),
    (gen_random_uuid(), sid, 'E10',    1.789, now() - interval '23 hours', 'available', now()),
    (gen_random_uuid(), sid, 'E85',    0.919, now() - interval '23 hours', 'available', now())
  ON CONFLICT (station_id, fuel_type) DO NOTHING;

  SELECT id INTO sid FROM stations WHERE source_id = 'FR-00032' AND source = 'gouvernement_francais';
  INSERT INTO fuel_prices (id, station_id, fuel_type, price_per_liter, reported_at, availability, data_synchronized_at) VALUES
    (gen_random_uuid(), sid, 'Gazole', 1.719, now() - interval '7 hours', 'available', now()),
    (gen_random_uuid(), sid, 'E10',    1.789, now() - interval '7 hours', 'available', now()),
    (gen_random_uuid(), sid, 'GPLc',   0.899, now() - interval '7 hours', 'available', now())
  ON CONFLICT (station_id, fuel_type) DO NOTHING;

  SELECT id INTO sid FROM stations WHERE source_id = 'FR-00033' AND source = 'gouvernement_francais';
  INSERT INTO fuel_prices (id, station_id, fuel_type, price_per_liter, reported_at, availability, data_synchronized_at) VALUES
    (gen_random_uuid(), sid, 'Gazole', 1.709, now() - interval '11 hours', 'available', now()),
    (gen_random_uuid(), sid, 'SP95',   1.859, now() - interval '11 hours', 'available', now()),
    (gen_random_uuid(), sid, 'E85',    0.939, now() - interval '11 hours', 'available', now())
  ON CONFLICT (station_id, fuel_type) DO NOTHING;

  SELECT id INTO sid FROM stations WHERE source_id = 'FR-00034' AND source = 'gouvernement_francais';
  INSERT INTO fuel_prices (id, station_id, fuel_type, price_per_liter, reported_at, availability, data_synchronized_at) VALUES
    (gen_random_uuid(), sid, 'Gazole', 1.729, now() - interval '16 hours', 'available', now()),
    (gen_random_uuid(), sid, 'E10',    1.799, now() - interval '16 hours', 'available', now())
  ON CONFLICT (station_id, fuel_type) DO NOTHING;

  SELECT id INTO sid FROM stations WHERE source_id = 'FR-00035' AND source = 'gouvernement_francais';
  INSERT INTO fuel_prices (id, station_id, fuel_type, price_per_liter, reported_at, availability, data_synchronized_at) VALUES
    (gen_random_uuid(), sid, 'Gazole', 1.749, now() - interval '9 hours', 'available', now()),
    (gen_random_uuid(), sid, 'E10',    1.819, now() - interval '9 hours', 'available', now()),
    (gen_random_uuid(), sid, 'SP98',   1.939, now() - interval '9 hours', 'available', now()),
    (gen_random_uuid(), sid, 'E85',    0.949, now() - interval '9 hours', 'available', now())
  ON CONFLICT (station_id, fuel_type) DO NOTHING;

END $$;
