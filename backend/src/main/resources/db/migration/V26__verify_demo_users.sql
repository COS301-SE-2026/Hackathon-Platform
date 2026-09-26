UPDATE users
SET email_verified = TRUE
WHERE email IN (
    'demo@admin.com',
    'part@demo.com'
    );