create table ultimo_evento_por_consulta (
                                            consulta_id  uuid primary key,
                                            ocorrido_em  timestamptz not null,
                                            tipo         varchar(100) not null
);