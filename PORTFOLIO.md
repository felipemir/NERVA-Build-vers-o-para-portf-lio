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
- Aumentaria testes de concorrência, isolamento entre lojas, idempotência e transições de estoque.
- Adicionaria CI, observabilidade, healthchecks e empacotamento da aplicação.
- Reduziria o MVP aos fluxos validados antes de introduzir funcionalidades de IA/visão.

## Limitações e cuidados

**Este repositório contém código de um MVP histórico, não pronto para produção.** Algumas migrations legadas incluem usuários fictícios com senha de demonstração em texto puro (`123456`); os dados não são credenciais reais, porém esse bootstrap é inseguro e precisaria ser removido ou substituído por hashes antes de um deploy real. As credenciais de PostgreSQL do `docker-compose.yml` e padrões de desenvolvimento são demonstrativas. O segredo JWT não é mais fornecido por padrão no arquivo `application.properties` e deve ser configurado por `APP_JWT_SECRET` com valor forte.

Não foram executados testes Maven nem implantada a aplicação durante a preparação do pacote para portfólio. O histórico Git do projeto original foi propositalmente removido do pacote para não expor metadados ou objetos antigos.
