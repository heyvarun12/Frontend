SELECT * FROM healthinsurancemanagement.support_ticket;
ALTER TABLE healthinsurancemanagement.support_ticket 
ADD COLUMN category VARCHAR(50) AFTER user_id,
ADD COLUMN priority VARCHAR(50) AFTER category;
ALTER TABLE healthinsurancemanagement.support_ticket 
ADD COLUMN category VARCHAR(50) DEFAULT 'General',
ADD COLUMN priority VARCHAR(50) DEFAULT 'Low';
ALTER TABLE healthinsurancemanagement.support_ticket 
ADD COLUMN subject VARCHAR(255) AFTER ticket_id;

ALTER TABLE healthinsurancemanagement.users 
ADD COLUMN phone VARCHAR(20),
ADD COLUMN join_date DATETIME;

Select * from healthinsurancemanagement.users;