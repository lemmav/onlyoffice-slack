CREATE OR REPLACE FUNCTION cleanup_stale_file_sessions()
RETURNS INTEGER AS $$
DECLARE
    deleted_count INTEGER;
BEGIN
    DELETE FROM active_file_sessions 
    WHERE created_at < NOW() - INTERVAL '30 days';
    GET DIAGNOSTICS deleted_count = ROW_COUNT;
    RETURN deleted_count;
END;
$$ LANGUAGE plpgsql;

SELECT cron.schedule(
    'cleanup_stale_sessions',
    '0 2 * * *',
    'SELECT cleanup_stale_file_sessions();'
);
