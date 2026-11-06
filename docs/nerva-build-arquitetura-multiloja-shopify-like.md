# Nerva Build - Arquitetura Multi-Loja com Experiencia Shopify-Like

## Objetivo Deste Documento

Este documento consolida a direcao de produto e arquitetura do Nerva Build a partir do que o backend ja atende hoje e do que ele pretende atender nas proximas evolucoes.

O objetivo e deixar claro que o projeto **nao precisa escolher entre multi-loja e experiencia Shopify-like**.

A direcao correta para o Nerva Build e:

- **multi-loja no nucleo operacional**
- **Shopify-like na experiencia publica de compra**

Em termos simples:

- o backoffice precisa continuar segregado por loja;
- a vitrine publica precisa parecer a loja da empresa;
- o comprador deve navegar e comprar como em uma storefront moderna;
- cada empresa deve continuar vendo apenas seus produtos, pedidos, clientes e operacao.

---

## Resumo Executivo

O Nerva Build e uma plataforma B2B para empresas de materiais de construcao venderem online e operarem seus pedidos com controle.

Cada empresa vendedora funciona como uma **loja independente dentro da plataforma**.

Isso significa que:

- cada loja possui seu proprio catalogo;
- cada loja possui seus proprios pedidos;
- cada loja possui seus proprios clientes operacionais;
- cada loja possui seus proprios usuarios internos;
- cada loja enxerga apenas sua propria operacao;
- compradores externos podem navegar e comprar da loja publica da empresa;
- o backend precisa sustentar essa separacao com seguranca e consistencia.

Ao mesmo tempo, a experiencia desejada para o comprador nao deve parecer um ERP ou um painel administrativo exposto. Ela deve parecer uma **storefront de ecommerce B2B**, com contexto publico da loja, navegacao clara, catalogo coerente e fluxo de compra simples.

Essa combinacao define a visao do projeto:

**Nerva Build e uma plataforma multi-tenant por loja, com storefront publica por empresa, orientada ao mercado B2B de materiais de construcao.**

---

## O Que Significa Multi-Loja Neste Projeto

No contexto do Nerva Build, multi-loja significa que o backend foi construído para operar varias empresas vendedoras no mesmo sistema, sem misturar dados operacionais.

Essa separacao ja aparece hoje no modelo e nas regras de negocio:

- `Store` e a raiz de segregacao;
- produtos pertencem a uma loja;
- categorias pertencem a uma loja;
- usuarios internos pertencem a uma loja;
- clientes locais pertencem a uma loja;
- pedidos pertencem a uma loja;
- itens de pedido pertencem a uma loja e a um pedido daquela loja.

Na pratica, isso permite que:

- uma empresa administre seu catalogo sem interferir em outra;
- uma empresa veja apenas seus pedidos;
- uma empresa gerencie apenas seus clientes operacionais;
- o painel administrativo seja isolado por tenant.

Esse modelo **deve ser mantido**.

Ele e especialmente adequado para o negocio do projeto, porque o Nerva Build nao pretende ser um catalogo unico compartilhado entre empresas, e sim uma plataforma em que cada empresa tenha sua propria operacao comercial.

---

## O Que Significa Shopify-Like Neste Projeto

No contexto do Nerva Build, Shopify-like nao significa copiar o Shopify literalmente.

Significa adotar alguns principios centrais:

- a loja publica deve ser tratada como uma unidade de experiencia;
- o comprador nao deve precisar pensar em `storeId`;
- a loja deve ser resolvida pelo contexto publico da requisicao;
- a navegacao publica deve parecer uma loja real;
- a identidade da empresa deve aparecer na vitrine;
- a plataforma deve permitir vender, receber pedidos e operar com fluidez;
- a camada publica deve ser simples para o comprador e consistente para o backend.

Em um modelo Shopify-like, o comprador acessa uma loja publica por:

- subdominio;
- dominio customizado;
- header controlado em ambientes especificos;
- ou fallback tecnico de ambiente quando necessario.

O comprador **nao deveria depender de um parametro funcional como `storeId`** para navegar.

No backend, isso se traduz em:

- resolver a loja publica pelo request;
- restringir catalogo e pedido ao contexto da loja resolvida;
- manter o contrato publico orientado a storefront;
- desacoplar a experiencia do comprador dos detalhes internos de multitenancy.

---

## A Tese Central do Projeto

O Nerva Build deve ser:

- **multi-loja no backoffice**
- **Shopify-like no storefront**

Essas duas ideias nao competem entre si. Elas se completam.

### No backoffice

Cada loja precisa continuar com:

- seu painel;
- seus usuarios internos;
- seus produtos;
- suas categorias;
- seus pedidos;
- seus clientes;
- sua visibilidade operacional.

### No storefront

Cada loja precisa parecer uma loja propria:

- com catalogo publico da empresa;
- com navegacao por categoria;
- com produto publico;
- com marca e contexto da loja;
- com jornada de compra simples;
- com pedido vinculado corretamente a empresa vendedora.

Essa combinacao e particularmente forte para um produto B2B de construcao, porque o mercado exige:

- independencia operacional por empresa;
- relacionamento comercial entre empresas;
- descoberta publica de catalogo;
- facilidade de compra;
- controle interno por tenant.

---

## Onde o Backend Ja Esta Bem Alinhado

O backend ja possui uma base relevante para essa visao.

### 1. Segregacao por loja

A principal fundacao ja existe.

Produtos, categorias, usuarios, clientes e pedidos possuem contexto de loja e regras para impedir acesso cruzado.

Isso ja coloca o projeto em uma boa base multi-tenant.

### 2. Contexto publico de loja

A `main` ja possui um servico especifico para resolver a loja publica:

- `PublicStoreContextService`

Esse servico ja aponta na direcao correta porque:

- aceita header publico;
- tenta extrair subdominio do host;
- possui fallback de loja padrao para MVP;
- retorna erro explicito quando a loja publica nao e encontrada.

Essa abordagem e muito mais proxima de uma storefront real do que depender sempre de `storeId` em query string.

### 3. Catalogo publico e administrativo separados

O projeto ja separa:

- endpoints publicos de produtos e categorias;
- endpoints administrativos de catalogo.

Essa separacao e importante porque o contrato publico da vitrine nao deve ser igual ao contrato do painel.

### 4. Pedido publico vinculado a loja

O fluxo de pedido publico ja resolve a loja e cria o pedido no contexto correto.

Isso e essencial para a visao de plataforma multi-loja: o comprador interage com a vitrine, mas o pedido continua pertencendo a loja vendedora.

### 5. Comprador global com espelho operacional local

A separacao entre:

- `CustomerAccount` global
- `Customer` local da loja

tambem e uma base muito promissora para B2B.

Ela permite evoluir para um modelo em que:

- o comprador tem identidade global na plataforma;
- as lojas mantem sua visao operacional local;
- um mesmo comprador pode negociar com mais de uma empresa.

---

## Onde o Backend Ainda Nao Esta Totalmente Shopify-Like

Apesar da boa fundacao, ainda ha pontos importantes de evolucao.

### 1. `storeId` ainda aparece no contrato publico

Hoje alguns endpoints publicos ainda aceitam `storeId` como entrada.

Isso funciona tecnicamente, mas nao e a melhor forma de modelar uma storefront publica.

Do ponto de vista do produto, `storeId` deveria ser:

- um detalhe interno do backend;
- ou no maximo uma compatibilidade temporaria.

No desenho-alvo, a loja deve ser determinada por contexto publico e nao por parametro funcional do cliente.

### 2. A vitrine ainda e mais API de catalogo do que storefront

O projeto ja possui catalogo publico, mas ainda falta consolidar a ideia de loja publica como unidade de experiencia.

Ainda faltam, em nivel de produto e backend:

- configuracoes publicas da loja;
- identidade de marca da empresa;
- dados de storefront por loja;
- rotas mais orientadas a experiencia da loja;
- elementos publicos de navegacao comercial.

### 3. O contexto publico ainda esta em fase MVP

O resolvedor atual ja vai na direcao certa, mas ainda esta em modo inicial.

Para uma evolucao mais madura, ele deve se tornar a fonte unica e confiavel para:

- host;
- subdominio;
- dominio customizado;
- header de ambiente quando aplicavel.

### 4. Falta uma camada explicita de configuracao da loja publica

Uma plataforma Shopify-like nao trata a loja apenas como um registro interno.

Ela trata a loja como uma entidade publica com:

- nome de exibicao;
- logo;
- banner;
- cores;
- descricao;
- canais;
- dominio;
- politicas;
- contato;
- configuracoes comerciais.

Hoje isso ainda nao esta modelado explicitamente no backend.

---

## O Que o Backend Ja Atende Hoje

Com base no estado atual da `main`, o backend ja atende:

### Operacao multi-loja

- segregacao de catalogo por loja;
- segregacao de pedidos por loja;
- segregacao de clientes por loja;
- segregacao de usuarios por loja;
- autorizacao administrativa por loja.

### Catalogo

- produtos por loja;
- categorias por loja;
- listagem publica;
- detalhe publico de produto;
- gestao administrativa de produtos;
- gestao administrativa de categorias.

### Pedidos

- criacao publica de pedido;
- validacao de estoque;
- snapshot de itens;
- associacao do pedido a loja;
- listagem administrativa;
- detalhe administrativo.

### Comprador

- conta global de comprador;
- autenticacao de comprador;
- identificacao do comprador no checkout;
- espelho operacional como cliente local da loja.

### Seguranca

- JWT stateless;
- separacao entre usuario interno e comprador;
- protecao das rotas administrativas;
- protecao por tenant no painel.

### Banco e integridade

- migrations com Flyway;
- constraints multi-loja;
- constraints de integridade comercial;
- base pronta para evolucao incremental.

---

## O Que o Backend Pretende Atender

O backend deve evoluir para atender uma visao mais completa da plataforma.

### 1. Storefront publica por empresa

Cada empresa deve ter sua propria loja publica.

O backend deve ser capaz de sustentar:

- home publica da loja;
- categorias publicas da loja;
- vitrine publica da loja;
- detalhe de produto da loja;
- busca publica;
- navegacao por contexto da loja;
- checkout publico da loja.

### 2. Resolucao unica de loja publica

O backend deve centralizar a resolucao de loja publica em uma unica estrategia oficial.

Essa estrategia deve priorizar:

- dominio customizado;
- subdominio;
- host;
- header controlado quando necessario;
- fallback tecnico apenas em ambiente de MVP.

### 3. Identidade publica da loja

O projeto deve evoluir para permitir que cada empresa tenha configuracao de storefront, como:

- nome publico;
- logo;
- identidade visual;
- descricao;
- dominio/subdominio;
- contato comercial;
- eventualmente politicas de entrega e atendimento.

### 4. Jornada B2B mais rica

Como o foco e B2B, o backend deve estar preparado para evoluir para:

- dados de empresa compradora;
- comprador profissional;
- arquitetos, engenheiros e compradores corporativos;
- condicoes comerciais futuras;
- fluxo entre empresas com isolamento de dados.

### 5. Operacao comercial mais madura

A plataforma ja possui definicao formal para o MVP operacional:

- transicao de status de pedido (PENDING → CONFIRMED → SEPARATING → SHIPPED → DELIVERED, com cancelamento);
- baixa automatica de estoque na confirmacao do pedido;
- restauracao de estoque no cancelamento;
- historico de movimentacao de estoque;
- historico de pedidos do comprador;
- paginacao em listagens;
- gestao de usuarios internos;
- edicao de perfil da loja.

Itens planejados para evolucoes posteriores ao MVP:

- pagamento integrado a gateway;
- auditoria operacional completa;
- historico formal de transicoes de status;
- relatorios por loja;
- configuracoes comerciais por loja.

---

## Diretriz Arquitetural Recomendada

Para manter coerencia com o que o backend ja faz bem, as proximas evolucoes devem seguir estas diretrizes.

### 1. Manter `storeId` no nucleo interno

`storeId` continua sendo essencial no dominio e no banco.

Ele deve permanecer como base de:

- persistencia;
- regras de negocio;
- autorizacao;
- relatorios;
- segregacao de tenant.

### 2. Remover `storeId` do contrato publico como solucao final

Na camada publica, `storeId` nao deve ser a forma principal de o cliente identificar a loja.

O frontend da vitrine nao deveria depender de:

- `GET /api/products?storeId=...`

como modelo final.

O backend deve resolver a loja por contexto publico.

### 3. Evitar estrategias paralelas de contexto publico

O projeto deve ter **uma unica estrategia oficial** para resolver a loja publica.

Nao e recomendavel:

- criar varios headers para a mesma ideia;
- usar `ThreadLocal` global para toda a API sem necessidade;
- manter logicas concorrentes em controller, interceptor e service.

O ideal e centralizar a regra em um servico claro e previsivel.

### 4. Separar storefront publica de backoffice

O backend deve tratar essas duas camadas como coisas diferentes:

- storefront publica;
- painel administrativo.

Mesmo quando compartilham o mesmo dominio de negocio, o contrato HTTP e a experiencia esperada sao diferentes.

### 5. Reaproveitar o que ja existe na `main`

As evolucoes futuras devem partir da arquitetura da `main`, nao de mecanismos paralelos de branches antigas.

Em especial:

- aproveitar `PublicStoreContextService`;
- estender os endpoints publicos com essa regra;
- migrar testes para esse contrato;
- evitar novos atalhos arquiteturais fora dessa base.

---

## Como Isso Se Aplica ao Projeto Hoje

Com base no codigo revisado ate aqui, a leitura mais correta do projeto e:

### O que deve continuar existindo

- multi-loja no modelo de dados;
- tenant por loja no painel;
- visibilidade por loja nos pedidos;
- produtos por loja;
- categorias por loja;
- clientes operacionais por loja;
- usuarios internos por loja.

### O que deve evoluir

- vitrine publica sem dependencia de `storeId`;
- contexto publico por loja usando a regra oficial da `main`;
- storefront mais orientada por dominio/subdominio;
- camada publica com mais cara de loja e menos cara de API administrativa.

### O que deve ser evitado

- contexto publico paralelo fora da estrategia oficial;
- contratos publicos baseados em detalhes internos do banco;
- mistura entre escopo administrativo e escopo publico;
- multiplos mecanismos para descobrir a loja da requisicao.

---

## Modelo Alvo do Produto

O modelo-alvo do Nerva Build pode ser descrito assim:

### Plataforma

Uma plataforma B2B multi-tenant para empresas de materiais de construcao venderem online.

### Tenant

Cada empresa vendedora e uma loja.

### Storefront

Cada loja possui uma vitrine publica propria, com contexto de marca, catalogo e compra.

### Comprador

O comprador pode ser uma pessoa ou profissional com identidade global na plataforma.

### Operacao

Cada loja opera seus proprios pedidos, clientes, estoque e atendimento.

### Resultado esperado

O comprador tem experiencia de loja moderna.

A empresa vendedora tem controle operacional isolado.

O backend sustenta os dois lados sem misturar contexto.

---

## Roadmap Arquitetural Recomendado

### Curto prazo (MVP operacional em andamento)

- implementar transicao de status do pedido com validacao de fluxo;
- implementar baixa e restauracao automatica de estoque vinculada ao status;
- criar historico de pedidos do comprador (listagem e detalhe);
- adicionar paginacao em todas as listagens;
- criar gestao basica de usuarios internos (listar, desativar, reativar);
- criar endpoint de edicao de perfil da loja;
- consolidar o uso de `PublicStoreContextService` nos endpoints publicos de vitrine;
- manter o painel administrativo totalmente orientado por tenant.

### Medio prazo

- reduzir a dependencia de `storeId` nos contratos publicos;
- criar configuracoes publicas da loja (logo, cores, banner);
- modelar storefront da empresa;
- evoluir home publica, categorias publicas e produto publico;
- fortalecer navegacao de loja por contexto.

### Longo prazo

- suportar dominios customizados;
- suportar identidade visual por loja;
- aprofundar fluxo B2B entre empresas;
- integrar pagamento com gateway;
- auditoria operacional e relatorios.

---

## Conclusao

O Nerva Build deve continuar sendo multi-loja.

Essa separacao e correta para o negocio, para a seguranca dos dados e para a operacao de cada empresa.

Ao mesmo tempo, o produto deve evoluir para uma experiencia de compra e vitrine semelhante a uma plataforma Shopify-like, especialmente na camada publica.

Portanto, a direcao oficial do projeto deve ser:

- **multi-loja no backoffice**
- **storefront Shopify-like na camada publica**

Essa nao e uma contradicao. Essa e justamente a combinacao que melhor atende o contexto B2B de materiais de construcao para o qual o Nerva Build esta sendo construido.
