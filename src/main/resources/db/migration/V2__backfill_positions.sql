INSERT INTO positions (instrument_id)
SELECT id FROM instruments
    ON CONFLICT (instrument_id) DO NOTHING;