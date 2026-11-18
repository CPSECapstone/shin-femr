# --- !Ups
CREATE TABLE inventory_import_history (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    trip_id INT NOT NULL,
    user_id INT NOT NULL,
    uploaded_by VARCHAR(255) NOT NULL,
    filename VARCHAR(255) NOT NULL,
    uploaded_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    successful BOOLEAN NOT NULL,
    medications_added INT NULL,
    medications_updated INT NULL,
    INDEX inventory_import_history_trip (trip_id, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

# --- !Downs
DROP TABLE inventory_import_history;
