create table mensagens_processadas (
             message_id   uuid primary key,
             tipo         varchar(100) not null,
             recebida_em  timestamptz not null
);

create table notificacoes (
             id           uuid primary key,
             message_id   uuid not null unique references mensagens_processadas (message_id),
             consulta_id  uuid not null,
             tipo         varchar(100) not null,
             evento       jsonb not null,
             status       varchar(20) not null,
             criada_em    timestamptz not null,
             enviada_em   timestamptz
);

create index ix_notificacoes_pendentes on notificacoes (criada_em)
    where status = 'PENDENTE';