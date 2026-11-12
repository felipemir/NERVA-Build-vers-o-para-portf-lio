# NERVA Build — registro de portfólio

**Status:** MVP experimental pausado. Não é um serviço em produção.

## Problema

Plataforma inicialmente concebida para apoiar lojas do segmento de materiais de construção, com catálogo por loja, pedidos e controle de estoque. A visão de longo prazo incluía assistência para planejamento de reformas e conexão com lojas, mas **a integração com LLM e a medição de ambientes por câmera não fazem parte deste backend entregue**.

## Minha contribuição

As decisões de produto e arquitetura eram compartilhadas com a equipe. Fui um dos principais responsáveis pela implementação do backend e pelo entendimento das regras do mercado de materiais de construção. O projeto foi pausado devido a mudanças na disponibilidade e nas prioridades da equipe, não por ausência de interesse na validação inicial realizada em Manaus.

## Decisões técnicas ilustradas

- Java/Spring Boot, Spring Security, JWT e PostgreSQL.
- Camadas de controllers, services, repositories e DTOs.
- Separação de dados por loja (abordagem multi-tenant orientada a `store_id`).
- Pedidos com snapshots de itens e regras de transição de status.
- Baixa/restauração de estoque em transições previstas pelo domínio, com transações e bloqueios.
- Migrations Flyway e alguns testes com Spring Boot/MockMvc.

## O que faria diferente hoje

- Endureceria segurança, remoção de contas e senhas de demonstração, segredos obrigatórios e controle de acesso multi-tenant.
- Aumentaria testes específicos de concorrência, idempotência e cenários extremos de estoque.
- Adicionaria CI, observabilidade, healthchecks e empacotamento da aplicação.
- Reduziria o MVP aos fluxos validados antes de introduzir funcionalidades de IA/visão.

## Limitações e cuidados

**Este repositório contém código de um MVP histórico, não pronto para produção.** Migrations legadas ainda exibem a senha fictícia `123456` durante o bootstrap, mas a migration `V12` converte essas ocorrências para BCrypt antes do estado final do banco. As credenciais de PostgreSQL do `docker-compose.yml` são exclusivamente locais. O segredo JWT não possui valor padrão e deve ser configurado por `APP_JWT_SECRET` com valor forte.

Durante a preparação desta versão, a suíte foi executada com PostgreSQL 16 via Testcontainers: 37 testes passaram sem falhas ou erros. O histórico Git do projeto original foi propositalmente removido do pacote para não expor metadados ou objetos antigos.
