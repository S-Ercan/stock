CREATE TABLE time_series (
    id INT PRIMARY KEY AUTO_INCREMENT,
    trading_day DATE,
    opening_price DOUBLE,
    high DOUBLE,
    low DOUBLE,
    closing_price DOUBLE,
    volume BIGINT
);
