alter table users alter column telefone drop not null;

create unique index users_email_unique_idx on users (email);
