# Nerva Build - Documentacao do Backend

## Visao Geral

O Nerva Build e o backend da plataforma de operacao comercial para lojas de materiais de construcao. No MVP atual, a Obra Ja e o primeiro cliente atendido por uma storefront propria e customizada, consumindo o backend do Nerva Build sem aparecer na home/catalogo global da plataforma.

O projeto foi estruturado como uma API Spring Boot com separacao clara entre:

- catalogo publico;
- painel administrativo da loja;
- compradores globais da plataforma;
- clientes locais da loja;
- pedidos publicos;
- operacao administrativa de pedidos.

O foco atual do MVP e permitir que uma loja publique seu catalogo, receba pedidos publicos, vincule esses pedidos a compradores validos e permita que a operacao visualize a fila e o detalhe dos pedidos.

O desenho atual separa dois mundos:

- storefront customizada da loja, como a Obra Ja;
- home/catalogo global do Nerva Build, que so deve exibir lojas explicitamente marcadas como visiveis.

## Stack

- Java 21
- Spring Boot 4
- Spring Web MVC
- Spring Data JPA
- Spring Security
- JWT
- PostgreSQL
- Flyway
- Maven
- Bean Validation
- Lombok

## Arquitetura

O projeto segue uma arquitetura em camadas:

- `controller`: recebe as requisicoes HTTP, aplica validacao de DTO e chama os servicos.
- `service`: concentra regras de negocio, validacoes, calculos e orquestracao.
- `repository`: acessa dados via Spring Data JPA.
- `domain/model`: contem as entidades persistidas.
- `dto`: define contratos de entrada e saida da API.
- `security`: contem autenticacao JWT e representacoes dos usuarios autenticados.
- `config`: contem configuracoes de seguranca e CORS.
- `exception`: contem excecoes de validacao especificas da API.
- `db/migration`: contem evolucoes de schema gerenciadas pelo Flyway.

Controllers nao acessam banco diretamente. Regras de negocio ficam nos services. Entidades nao devem ser expostas diretamente como contrato publico da API quando houver DTO especifico.

## Multi-Loja

A plataforma e multi-tenant por loja.

A entidade central para segregacao e `Store`. Produtos, categorias, usuarios, clientes locais, pedidos e itens de pedido carregam ou derivam um `storeId`.

No painel administrativo, a loja ativa vem sempre do usuario autenticado. O frontend administrativo nao escolhe livremente a loja por parametro.

No fluxo publico do MVP, a loja e resolvida por regra de contexto publico:

- header `X-Public-Store`, quando informado;
- subdominio do host, quando aplicavel;

Nao existe mais fallback automatico para a `obraja`. Isso evita que chamadas publicas sem contexto caiam no cliente MVP por acidente.

A Obra Ja continua podendo consumir o backend normalmente pelo frontend customizado, desde que o frontend informe explicitamente o contexto da loja, por exemplo:

- `X-Public-Store: obraja` em chamadas de pedido;
- `storeId` da Obra Ja em rotas publicas de catalogo que ainda usam query string.

Para a home/catalogo global da plataforma, existe a flag `marketplaceVisible` na loja. Apenas lojas com `marketplaceVisible=true` aparecem no marketplace global. Por padrao, lojas criadas via onboarding nascem com `marketplaceVisible=false`.

## Seguranca

A API usa JWT stateless.

Existem dois tipos de principal autenticado:

- usuario interno da loja;
- comprador global da plataforma.

Usuarios internos acessam painel, catalogo administrativo, clientes locais e pedidos administrativos.

Compradores globais acessam fluxos de comprador, como login, cadastro e potencialmente criacao de pedido publico autenticado.

Rotas publicas atuais:

- login administrativo;
- cadastro e login de comprador;
- catalogo publico de produtos;
- categorias publicas;
- onboarding publico de loja;
- marketplace publico de produtos;
- criacao de pedido para comprador autenticado.

Demais rotas exigem autenticacao.

O projeto nao usa mais Basic Auth como regra principal. A autenticacao operacional atual e via token Bearer JWT.

## Usuarios Internos da Loja

Usuarios internos ficam na tabela `users`.

Cada usuario interno pertence a uma loja por `storeId`.

Papeis reconhecidos no modelo:

- `SUPER_ADMIN`
- `STORE_OWNER`
- `STORE_ADMIN`
- `STORE_OPERATOR`
- `CUSTOMER_B2B`
- `CUSTOMER`
- `SELLER`
- `STOCK_MANAGER`

No MVP, as permissoes administrativas principais usam:

- `STORE_OWNER`
- `STORE_ADMIN`
- `STORE_OPERATOR`

Permissoes atuais:

- `STORE_OWNER`: gerencia loja, usuarios internos, catalogo, clientes e pedidos;
- `STORE_ADMIN`: gerencia loja, usuarios operadores, catalogo, clientes e pedidos;
- `STORE_OPERATOR`: gerencia catalogo e clientes, para apoiar produto e atendimento, mas nao acessa gestao de lojas nem pedidos administrativos.

O cadastro de novos usuarios internos respeita hierarquia:

- `STORE_OWNER` pode criar `STORE_ADMIN` e `STORE_OPERATOR`;
- `STORE_ADMIN` pode criar `STORE_OPERATOR`;
- usuarios nao podem cadastrar pessoas em outra loja.

O login administrativo exige email, senha e loja. Senhas antigas em texto podem ser migradas automaticamente para BCrypt no login bem sucedido.

## Comprador Global

O comprador final da plataforma e representado por `CustomerAccount`.

Essa conta e global, ou seja, nao pertence a uma loja especifica. Essa decisao prepara a plataforma para um comprador poder comprar em varias lojas sem precisar ter uma identidade separada em cada uma.

Campos principais do comprador global:

- nome;
- email;
- senha;
- documento opcional;
- celular;
- tipo de pessoa;
- ativo;
- datas de criacao e atualizacao.

No fluxo publico de pedido, a identidade principal do comprador passou a ser `CustomerAccount`.

Regra atual do MVP:

- pedido publico nao pode ser criado sem comprador autenticado;
- nome, email, celular e endereco sao obrigatorios no pedido publico;
- documento ainda e opcional;
- o backend localiza comprador global por email;
- se documento for informado, tambem pode localizar por documento;
- todo pedido publico passa a receber `customerAccountId`.

Essa regra evita tratar o comprador autenticavel como cliente local restrito a uma loja.

## Cliente Local da Loja

A entidade `Customer` continua existindo como cliente local da loja.

Ela e usada pelo painel administrativo e tambem como espelho operacional do comprador dentro da loja.

No pedido publico, o `Customer` local nao e mais a identidade principal do comprador. Ele continua sendo criado ou reutilizado para manter dados operacionais da loja, especialmente endereco, telefone, nome e relacionamento com o pedido.

Essa convivencia permite:

- comprador global como identidade da plataforma;
- cliente local como visao operacional da loja;
- evolucao futura para vinculos formais entre comprador global e loja sem bloquear o MVP.

## Catalogo

O catalogo e composto por produtos e categorias.

Produtos pertencem a uma loja. Categorias tambem pertencem a uma loja.

Regras principais de produto:

- produto tem loja obrigatoria;
- nome e preco sao obrigatorios;
- unidade de venda e obrigatoria;
- marca e opcional;
- estoque nao pode ser negativo;
- preco nao pode ser negativo;
- peso nao pode ser negativo quando informado;
- dimensoes em centimetros sao opcionais e nao podem ser negativas;
- quantidade minima de pedido pode ser definida;
- produto informa se permite entrega e/ou retirada;
- SKU e unico por loja quando preenchido;
- produto pode ser ativado ou desativado;
- produto desativado nao aparece no catalogo publico;
- categoria do produto precisa pertencer a mesma loja.

Catalogo publico:

- lista apenas produtos ativos;
- permite filtro por termo;
- permite filtro por categoria;
- exige `storeId` nas rotas publicas de catalogo especifico de loja;
- nao usa mais fallback automatico para a Obra Ja;
- marketplace global lista apenas produtos de lojas com `marketplaceVisible=true`.

Catalogo administrativo:

- usa a loja do usuario autenticado;
- lista produtos ativos e inativos;
- permite criar, editar e desativar produtos;
- nao permite gerenciar produto de outra loja.

Categorias:

- possuem nome, slug, descricao, status ativo e loja;
- slug e unico por loja;
- categorias publicas retornam apenas ativas;
- categorias publicas exigem `storeId`;
- painel administrativo enxerga todas da loja.

## Pedido Publico

O endpoint publico de criacao de pedido e a entrada principal do fluxo da Obra Ja no MVP.

Fluxo resumido:

- resolve loja publica por `X-Public-Store` ou subdominio do host;
- exige comprador autenticado por JWT;
- valida comprador;
- usa o comprador global autenticado;
- cria ou reutiliza cliente local operacional;
- valida itens;
- busca produtos ativos da loja correta;
- valida estoque disponivel;
- calcula preco unitario, subtotal e total no backend;
- cria pedido com status inicial;
- cria itens com snapshots historicos;
- retorna dados principais do pedido criado.

Regras do comprador no pedido publico:

- nome obrigatorio;
- email obrigatorio;
- celular obrigatorio;
- endereco obrigatorio;
- documento opcional;
- token de comprador e obrigatorio;
- comprador global e sempre vinculado ao pedido;
- cliente local e mantido como espelho operacional.

Para frontends customizados, como a Obra Ja, o frontend deve sempre enviar:

- `Authorization: Bearer <customerToken>`;
- `X-Public-Store: obraja`.

Sem contexto explicito de loja, o backend retorna erro e nao assume a Obra Ja automaticamente.

Regras dos itens:

- lista de itens nao pode ser vazia;
- cada item precisa ter produto e quantidade;
- quantidade precisa ser positiva;
- produtos duplicados no mesmo pedido sao rejeitados;
- produto precisa existir, estar ativo e pertencer a loja publica resolvida;
- frontend nao define preco final, subtotal ou total.

Status inicial:

- pedido nasce como `PENDING`;
- pagamento nasce como `PENDING`.

Numero de pedido:

- apos persistir o pedido, o backend gera um numero operacional no formato baseado em loja e id do pedido.

## Historico dos Itens do Pedido

Os itens do pedido salvam os dados historicos do produto no momento da compra.

Cada item guarda:

- produto relacionado;
- loja;
- pedido;
- quantidade;
- preco unitario praticado;
- subtotal calculado;
- SKU no momento da venda;
- nome do produto no momento da venda;
- unidade de medida.

Isso garante que mudancas futuras no cadastro do produto nao alterem o historico de pedidos ja realizados.

A tela de detalhe e a listagem operacional devem usar os dados de snapshot dos itens, nao leitura ao vivo do produto, para reconstruir historico comercial.

## Estoque

O backend valida disponibilidade de estoque antes de aceitar o pedido publico.

A regra atual do MVP:

- pedido acima do estoque disponivel e rejeitado;
- validacao usa sempre produto da loja correta;
- estoque nulo e tratado como indisponivel;
- mensagem de erro informa produto, quantidade disponivel e quantidade solicitada;
- regra esta centralizada em `StockService`.

### Politica de estoque definida para o MVP operacional

- pedido `PENDING` valida estoque, mas nao decrementa;
- transicao para `CONFIRMED` decrementa estoque de todos os itens;
- cancelamento de pedido que ja foi confirmado restaura estoque;
- cancelamento de pedido que era apenas PENDING nao altera estoque;
- toda operacao de estoque roda na mesma transacao da mudanca de status;
- locking pessimista protege concorrencia;
- cada decremento ou restauracao gera um registro em `stock_movements` com: produto, loja, pedido, quantidade, tipo (DECREMENT/RESTORATION) e timestamp;
- se qualquer produto ficar com estoque negativo, toda a confirmacao e rejeitada e nenhum item e afetado.

### Pontos futuros apos o MVP

- reserva de estoque separada do decremento;
- integracao com pagamento para decidir quando baixar definitivamente;
- alertas de estoque baixo;
- relatorios de movimentacao.

## Pedidos Administrativos

A operacao da loja possui endpoints administrativos para consultar pedidos.

Listagem operacional:

- lista apenas pedidos da loja autenticada;
- permite filtro por status;
- permite filtro por comprador;
- permite filtro por periodo;
- ordena por pedidos mais recentes;
- retorna dados suficientes para uma fila operacional.

Resumo retornado na listagem:

- id;
- numero do pedido;
- status;
- status de pagamento;
- total;
- comprador;
- contato do comprador;
- quantidade de itens;
- resumo dos itens;
- datas.

Detalhe operacional:

- consulta um pedido especifico por id;
- sempre restringe por loja autenticada;
- retorna dados principais;
- retorna comprador global e cliente local quando aplicavel;
- retorna endereco salvo no cliente local;
- retorna itens com snapshots;
- retorna totais, subtotais, status, datas e observacoes;
- nao permite consultar pedido de outra loja.

O detalhe do pedido e a fonte principal para a tela administrativa do MVP.

## Status de Pedido

Hoje o pedido nasce com status `PENDING`.

O banco ja aceita outros status relevantes para a operacao:

- `PENDING`
- `CONFIRMED`
- `SEPARATING`
- `SHIPPED`
- `DELIVERED`
- `CANCELED`
- `PAID`

### Decisoes definidas para o MVP operacional

Fluxo de transicao oficial:

- `PENDING` para `CONFIRMED`;
- `CONFIRMED` para `SEPARATING`;
- `SEPARATING` para `SHIPPED`;
- `SHIPPED` para `DELIVERED`;
- `PENDING`, `CONFIRMED` ou `SEPARATING` para `CANCELED`.

Status terminais (nao permitem nova transicao): `DELIVERED` e `CANCELED`.

Quem pode mudar status: apenas `STORE_OWNER` e `STORE_ADMIN`. `STORE_OPERATOR` nao pode.

O endpoint de transicao registra quem fez a mudanca (`updatedByUserId`) e atualiza `updatedAt`.

### Politica de estoque vinculada ao status

- `PENDING`: valida estoque, mas nao decrementa;
- `CONFIRMED`: decrementa estoque automaticamente;
- `CANCELED` (se ja foi CONFIRMED+): restaura estoque;
- `CANCELED` (se era PENDING): nao altera estoque;
- Concorrencia protegida por locking pessimista;
- Toda movimentacao gera registro historico em `stock_movements`.

### Pontos que permanecem para evolucao futura

- como gateway de pagamento vai interagir com status operacional e `paymentStatus`;
- historico formal de transicoes de status (tabela de audit trail).

## Pagamento

O projeto separa status operacional do pedido e status de pagamento.

O pedido possui:

- `status`, representando ciclo operacional;
- `paymentStatus`, representando pagamento;
- `paymentMethod`, quando aplicavel.

No MVP atual, pagamento ainda nao esta integrado a gateway.

O desenho permite evoluir para:

- iniciar pagamento apos criacao do pedido;
- receber webhook do gateway;
- atualizar `paymentStatus`;
- confirmar ou liberar estoque conforme regra futura;
- manter status logistico separado do pagamento.

## Erros e Validacao

A API possui resposta padronizada de erro.

Erros de validacao de DTO e erros de regra de negocio usam o mesmo envelope basico, contendo:

- data/hora;
- codigo HTTP;
- nome do erro;
- mensagem;
- path da requisicao;
- lista de campos com problema.

Exemplos de erros tratados:

- campos obrigatorios ausentes;
- status invalido;
- produto inexistente;
- produto de outra loja;
- estoque insuficiente;
- comprador invalido;
- documento duplicado;
- conflitos de integridade no banco.

## Banco de Dados e Migrations

O projeto usa Flyway para versionamento do banco.

Migrations principais:

- `V1`: cria loja e produto base;
- `V2`: adiciona categoria textual ao produto;
- `V3`: cria usuarios, clientes, pedidos e itens;
- `V4`: endurece schema do MVP, categorias, integridade multi-loja, pedidos e auditoria;
- `V5`: cria seeds e defaults operacionais;
- `V6`: cria conta global de comprador;
- `V7`: prepara pedidos para referenciar comprador global;
- `V8`: adiciona snapshots de comprador e endereco no pedido;
- `V9`: adiciona campos de produto para materiais de construcao;
- `V10`: adiciona visibilidade da loja no marketplace.

O banco possui constraints importantes para proteger multi-loja:

- produto por loja;
- categoria por loja;
- cliente por loja;
- pedido por loja;
- item de pedido por loja e pedido;
- item de pedido por loja e produto.

## Regras Importantes de Segregacao

O backend sempre deve respeitar estas regras:

- rotas administrativas usam loja do usuario autenticado;
- frontend administrativo nao deve enviar `storeId` para acessar dados de outra loja;
- pedido publico usa contexto de loja publica;
- produto de outra loja nao pode entrar em pedido;
- pedido de outra loja nao pode aparecer em listagem ou detalhe;
- cliente local pertence a uma loja;
- comprador global nao pertence a uma loja.

## Endpoints Principais

Autenticacao administrativa:

- `POST /api/auth/login`
- `GET /api/auth/me`
- `POST /api/auth/register`

Autenticacao de comprador:

- `POST /api/customer-auth/register`
- `POST /api/customer-auth/login`
- `GET /api/customer-auth/me`

Catalogo publico:

- `GET /api/marketplace/products`
- `GET /api/products?storeId={storeId}`
- `GET /api/products/{id}?storeId={storeId}`
- `GET /api/categories?storeId={storeId}`
- `GET /api/categories/slug/{slug}?storeId={storeId}`

Catalogo administrativo:

- `POST /api/products`
- `GET /api/products/admin`
- `GET /api/products/admin/{id}`
- `PUT /api/products/{id}`
- `DELETE /api/products/{id}`
- `GET /api/categories/admin`
- `GET /api/categories/admin/{id}`
- `POST /api/categories`
- `PUT /api/categories/{id}`
- `DELETE /api/categories/{id}`

Clientes locais da loja:

- `POST /api/customers`
- `GET /api/customers`
- `GET /api/customers/{id}`
- `PUT /api/customers/{id}`
- `DELETE /api/customers/{id}`

Pedidos:

- `POST /api/public/orders`
- `GET /api/orders/admin`
- `GET /api/orders/admin/{id}`

Lojas:

- `POST /api/stores`
- `POST /api/stores/register`
- `GET /api/stores`

Observacao: `POST /api/stores/register` e publico e cria loja + primeiro `STORE_OWNER`. As rotas administrativas de loja exigem `STORE_OWNER` ou `STORE_ADMIN`.

## Como Rodar Localmente

Requisitos:

- Java 21;
- PostgreSQL;
- Maven Wrapper do projeto.

Configuracao padrao em `application.properties`:

- banco `nerva_db`;
- porta PostgreSQL `5433`;
- usuario `postgres`;
- senha `postgres`;
- JWT configurado por `app.jwt.secret`.

Passos gerais:

- subir PostgreSQL com o banco configurado;
- conferir `application.properties`;
- rodar a aplicacao Spring Boot;
- o Flyway aplica migrations automaticamente;
- usar login administrativo para obter Bearer token;
- chamar endpoints administrativos com token.

Para testar uma storefront customizada da Obra Ja, o frontend ou Postman deve informar explicitamente o contexto:

- catalogo: `GET /api/products?storeId={obrajaStoreId}`;
- categorias: `GET /api/categories?storeId={obrajaStoreId}`;
- pedido: header `X-Public-Store: obraja`.

## Testes

A validacao basica atual e feita com Maven.

O comando usado durante as entregas recentes foi `mvnw.cmd test`.

O teste atual sobe o contexto Spring, valida beans, repositories, JPA, Flyway e configuracao basica.

Ainda ha espaco para evoluir testes especificos de:

- criacao de pedido publico;
- validacao de comprador global;
- snapshot de itens;
- estoque insuficiente;
- listagem admin por loja;
- detalhe admin por loja;
- tentativa de acesso a pedido de outra loja;
- futuras transicoes de status.

## Pontos de Evolucao Ja Mapeados

### Em implementacao (MVP operacional)

Fluxo de status:

- endpoint administrativo para atualizacao de status;
- validacao de transicoes permitidas;
- registro do usuario responsavel pela mudanca.

Estoque:

- baixa automatica na confirmacao;
- restauracao no cancelamento;
- locking pessimista;
- tabela de movimentacao historica.

Comprador:

- historico de pedidos do comprador;
- detalhe do proprio pedido com status e itens.

Paginacao:

- offset-based em todas as listagens (admin e publica);
- Page_Response padronizado.

Gestao de equipe:

- listagem de usuarios internos por loja;
- detalhe, desativacao e reativacao;
- respeito a hierarquia de papeis.

Perfil da loja:

- edicao de dados da loja (nome, telefone, email, CNPJ, endereco);
- validacao de campos brasileiros.

### Evolucoes futuras (pos-MVP)

Pagamento:

- integrar gateway;
- tratar webhooks;
- separar pagamento aprovado, recusado, expirado e reembolsado;
- coordenar pagamento com status operacional.

Comprador global:

- criar fluxo para comprador completar cadastro;
- permitir redefinir senha de conta criada no checkout rapido;
- evoluir enderecos globais;
- criar vinculo formal comprador-loja se necessario.

Operacao avancada:

- historico formal de transicoes de status;
- auditoria operacional;
- dashboards e relatorios simples.

Storefront:

- configuracao publica de storefront (logo, cores, banner);
- dominio customizado salvo no banco;
- endpoint publico para resolver loja por subdominio/dominio.

Seguranca:

- endurecer rotas de loja;
- revisar permissoes por perfil;
- configurar segredo JWT por ambiente;
- remover qualquer seed sensivel de producao.

## Estado Atual do MPV/MVP

O backend ja tem uma base funcional para o primeiro MVP com a Obra Ja como cliente inicial isolado e com possibilidade de evoluir para novas lojas.

### Visao do cliente comprador

O comprador ja consegue:

- cadastrar conta global;
- fazer login;
- consultar dados da propria conta;
- navegar catalogo publico de uma loja especifica quando o frontend informa `storeId`;
- consultar categorias publicas de uma loja especifica;
- ver detalhe publico de produto;
- criar pedido autenticado;
- comprar em uma storefront customizada quando o frontend envia `X-Public-Store`;
- ter pedido vinculado a conta global e a um cliente local operacional da loja.

Ainda falta para uma experiencia mais completa do comprador:

- enderecos salvos;
- fluxo de pagamento;
- storefront publica sem dependencia de `storeId` nas rotas de catalogo.

Em implementacao para o MVP operacional:

- historico de pedidos do comprador;
- detalhe do proprio pedido;
- acompanhamento de status.

### Visao da loja

A loja ja consegue:

- criar loja + primeiro `STORE_OWNER` via onboarding;
- fazer login no painel;
- criar `STORE_ADMIN` e `STORE_OPERATOR`;
- cadastrar categorias;
- cadastrar produtos com campos adequados para materiais de construcao, como unidade, marca, peso, dimensoes, quantidade minima, entrega e retirada;
- manter produtos ativos/inativos;
- atender clientes locais pelo painel;
- operar como loja isolada consumida por frontend customizado.

Para a Obra Ja, o comportamento desejado do MVP e:

- a loja existe no backend;
- a storefront customizada consome o backend;
- pedidos usam `X-Public-Store: obraja`;
- catalogo usa o `storeId` da Obra Ja enquanto nao houver endpoint publico de loja atual;
- a Obra Ja nao aparece no marketplace/home global do Nerva Build.

Ainda falta para loja:

- configuracao publica de storefront, como logo, cores, banner e contatos;
- dominio customizado salvo no banco;
- endpoint publico para resolver loja por subdominio/dominio e devolver `storeId`.

Em implementacao para o MVP operacional:

- edicao completa dos dados da loja (nome, telefone, email, CNPJ, endereco);
- gestao de equipe com listagem, desativacao e reativacao de usuarios internos.

### Visao do admin/operacao

O painel administrativo ja consegue:

- autenticar usuario por loja;
- identificar usuario logado via `/api/auth/me`;
- listar lojas para `STORE_OWNER` e `STORE_ADMIN`;
- gerenciar catalogo por loja;
- gerenciar categorias por loja;
- gerenciar clientes locais;
- listar pedidos da propria loja;
- filtrar pedidos por status, comprador e periodo;
- consultar detalhe operacional do pedido;
- visualizar snapshots de itens e dados do comprador/endereco.

Permissoes atuais:

- `STORE_OWNER`: administra loja, usuarios, catalogo, clientes e pedidos;
- `STORE_ADMIN`: administra loja, operadores, catalogo, clientes e pedidos;
- `STORE_OPERATOR`: atua em produtos, categorias e clientes, mas nao acessa gestao de lojas nem pedidos administrativos.

Ainda falta para admin/operacao:

- dashboards e indicadores;
- filtros mais completos;
- integracao de pagamento.

Em implementacao para o MVP operacional:

- atualizar status do pedido com validacao de fluxo;
- baixa e restauracao automatica de estoque;
- registro de quem alterou o status;
- paginacao em todas as listas.
