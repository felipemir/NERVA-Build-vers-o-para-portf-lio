# 02. Arquitetura Técnica: Padronização Backend ⚙️

---

## 1. Stack Tecnológica Oficial

| Tecnologia | Versão | Propósito |
|------------|--------|-----------|
| Java | 21 (LTS) | Linguagem principal |
| Spring Boot | 4.0.2 | Framework web |
| Spring Web MVC | — | Camada REST |
| Spring Data JPA | — | Acesso a dados |
| Spring Security | — | Autenticação e autorização |
| Bean Validation | — | Validação de DTOs |
| JWT (jjwt) | 0.12.6 | Tokens stateless |
| PostgreSQL | 16 | Banco de dados |
| Flyway | — | Migrations de schema |
| Lombok | — | Redução de boilerplate |
| Maven Wrapper | — | Build tool (não precisa instalar Maven global) |

> O projeto usa `mvnw` (Linux/Mac) ou `mvnw.cmd` (Windows). Não precisa instalar Maven globalmente.

---

## 2. Estrutura de Pastas (Organização do Código)

O backend está padronizado sob o pacote raiz `com.nervatech.nerva_build`. Cada camada tem uma responsabilidade única:

```
src/main/java/com/nervatech/nerva_build/
├── config/          # Configurações de Beans, Segurança e CORS
├── controller/      # Endpoints REST (recebe requisições, valida DTO, chama service)
├── domain/model/    # Entidades JPA que representam as tabelas do banco
├── dto/             # Objetos de transporte de dados (Request/Response)
├── exception/       # Exceções customizadas da API
├── repository/      # Interfaces de acesso ao banco (Spring Data JPA)
├── security/        # Filtro JWT, JwtService, principals (AppUserDetails, CustomerAccountDetails)
├── service/         # Regras de negócio, validações, cálculos e orquestração
└── NervaBuildApplication.java  # Classe principal (Start do sistema)
```

### Outros diretórios importantes:

```
src/main/resources/
├── application.properties          # Configurações da aplicação
└── db/migration/                   # Scripts SQL de evolução do banco (Flyway)

src/test/java/                      # Testes unitários e de integração
```

---

## 3. Regras de Ouro para Desenvolvedores

### 3.1. Lógica é no Service

O controller apenas:
- recebe a requisição HTTP
- valida o DTO (via `@Valid`)
- extrai o usuário autenticado (`@AuthenticationPrincipal`)
- chama o service
- retorna o ResponseEntity

**Nunca** coloque lógica de cálculo, validação de negócio ou acesso a banco no controller.

```java
// ✅ Correto
@PostMapping
public ResponseEntity<OrderResponseDTO> create(@Valid @RequestBody OrderRequestDTO dto) {
    return ResponseEntity.status(HttpStatus.CREATED).body(orderService.create(dto));
}

// ❌ Errado — lógica no controller
@PostMapping
public ResponseEntity<Order> create(@RequestBody Order order) {
    if (order.getTotal() < 0) throw new RuntimeException("negativo");
    order.setStatus("PENDING");
    return ResponseEntity.ok(orderRepository.save(order));
}
```

### 3.2. DTO vs Entity

- **Nunca** retorne uma classe de `domain.model` diretamente na API.
- Use sempre um DTO para filtrar o que o frontend deve ver.
- DTOs de entrada usam sufixo `RequestDTO`.
- DTOs de saída usam sufixo `ResponseDTO`.
- Records são o padrão para DTOs neste projeto.

```java
// ✅ Padrão do projeto
public record ProductResponseDTO(Long id, String name, BigDecimal price, ...) {}

// ❌ Nunca retornar a entity
public ResponseEntity<Product> get() { ... }
```

### 3.3. Migrations

- Qualquer nova tabela ou coluna **DEVE** ter um novo arquivo `.sql` em `db/migration`.
- Formato: `V{N}__{descricao}.sql` (dois underscores).
- **Alterações manuais no banco de dados são proibidas.**
- Nunca edite uma migration já aplicada — crie uma nova.
- A aplicação usa `ddl-auto=validate`: o Hibernate apenas valida se o schema bate com as entities.

```
src/main/resources/db/migration/
├── V1__init_schema.sql
├── V2__add_category_to_product.sql
├── V3__create_users_customers_orders.sql
├── ...
└── V10__add_store_marketplace_visibility.sql
```

### 3.4. Validação de DTO

Use Bean Validation nos records de request:

```java
public record CategoryRequestDTO(
    @NotBlank(message = "Nome da categoria e obrigatorio")
    @Size(max = 100)
    String name,

    @NotBlank(message = "Slug e obrigatorio")
    String slug
) {}
```

O controller usa `@Valid` e o `ApiExceptionHandler` transforma erros em resposta padronizada.

### 3.5. Tratamento de Erros

O projeto usa um handler global (`ApiExceptionHandler`) que padroniza todas as respostas de erro:

```json
{
  "timestamp": "2026-06-27T10:30:00",
  "status": 422,
  "error": "Unprocessable Entity",
  "message": "Estoque insuficiente para o produto",
  "path": "/api/public/orders",
  "fieldErrors": [
    { "field": "items[0].quantity", "message": "Estoque disponivel: 5, solicitado: 10" }
  ]
}
```

Para lançar erros de negócio, use:
- `ResponseStatusException` para erros simples (404, 403, 401)
- `ApiValidationException` para erros com detalhes por campo

```java
// Erro simples
throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido nao encontrado");

// Erro com campos
throw new ApiValidationException("Erro de estoque", List.of(
    new ApiFieldErrorDTO("items[0]", "Estoque insuficiente: disponivel 5, solicitado 10")
));
```

---

## 4. Estratégia de Isolamento (Multi-tenant)

O sistema atende múltiplas lojas. **Toda entidade** que pertence a uma loja deve conter o campo `storeId`.

### Entidades segregadas por loja:

| Entidade | Campo | Descrição |
|----------|-------|-----------|
| `Product` | `storeId` | Produto pertence a uma loja |
| `Category` | `storeId` | Categoria pertence a uma loja |
| `AppUser` | `storeId` | Usuário interno pertence a uma loja |
| `Customer` | `storeId` | Cliente local pertence a uma loja |
| `Order` | `storeId` | Pedido pertence a uma loja |
| `OrderItem` | `storeId` | Item do pedido pertence a uma loja |

### Entidade global (não segregada):

| Entidade | Descrição |
|----------|-----------|
| `CustomerAccount` | Comprador global da plataforma (pode comprar em qualquer loja) |
| `Store` | A própria loja |

### Regras de segregação:

- Rotas administrativas **sempre** usam a loja do usuário autenticado (`currentUser.getStoreId()`).
- O frontend admin **nunca** envia `storeId` como parâmetro.
- Rotas públicas resolvem a loja por contexto (`X-Public-Store` header ou subdomínio).
- Queries no repository **sempre** filtram por `storeId`.

```java
// ✅ Correto — sempre filtra por loja
List<Product> findByStoreIdAndActiveTrue(Long storeId);

// ❌ Errado — retorna dados de todas as lojas
List<Product> findByActiveTrue();
```

---

## 5. Segurança e Autenticação

### 5.1. Modelo de Autenticação

O projeto usa **JWT stateless**. Não há sessão no servidor.

Existem **dois tipos de principal** autenticado:

| Principal | Classe | Uso |
|-----------|--------|-----|
| Usuário interno da loja | `AppUserDetails` | Painel administrativo |
| Comprador global | `CustomerAccountDetails` | Storefront / pedidos |

O `JwtAuthenticationFilter` identifica o tipo pelo claim `principalType` no token:
- `APP_USER` → carrega `AppUserDetails`
- `CUSTOMER` → carrega `CustomerAccountDetails`

### 5.2. Claims do Token (Usuário Interno)

```json
{
  "sub": "1",
  "principalType": "APP_USER",
  "email": "joao@obraja.com",
  "storeId": 1,
  "role": "STORE_OWNER",
  "iat": 1719480000,
  "exp": 1719566400
}
```

### 5.3. Claims do Token (Comprador)

```json
{
  "sub": "5",
  "principalType": "CUSTOMER",
  "email": "maria@email.com",
  "role": "CUSTOMER",
  "personType": "PF",
  "iat": 1719480000,
  "exp": 1719566400
}
```

### 5.4. Controle de Acesso nos Controllers

```java
// Obtendo o usuário admin autenticado
@GetMapping("/admin")
public ResponseEntity<?> list(@AuthenticationPrincipal AppUserDetails currentUser) {
    Long storeId = accessControlService.requireOrderStore(currentUser);
    return ResponseEntity.ok(orderService.listByStore(storeId));
}

// Obtendo o comprador autenticado
@GetMapping("/me")
public ResponseEntity<?> me(@AuthenticationPrincipal CustomerAccountDetails currentUser) {
    return ResponseEntity.ok(service.getMe(currentUser.getId()));
}
```

### 5.5. Papéis e Permissões

| Papel | Catálogo | Pedidos | Loja | Usuários |
|-------|----------|---------|------|----------|
| `STORE_OWNER` | ✅ | ✅ | ✅ | ✅ (cria admin/operator) |
| `STORE_ADMIN` | ✅ | ✅ | ✅ | ✅ (cria operator) |
| `STORE_OPERATOR` | ✅ | ❌ | ❌ | ❌ |

O `AccessControlService` centraliza a validação de permissão:

```java
// Verifica se pode gerenciar catálogo (OWNER, ADMIN, OPERATOR)
Long storeId = accessControlService.requireCatalogStore(currentUser);

// Verifica se pode gerenciar pedidos (OWNER, ADMIN apenas)
Long storeId = accessControlService.requireOrderStore(currentUser);

// Verifica se pode gerenciar loja (OWNER, ADMIN apenas)
Long storeId = accessControlService.requireStoreManagement(currentUser);
```

### 5.6. Rotas Públicas (sem token)

Configuradas no `SecurityConfig`:

- `POST /api/auth/login` — login admin
- `POST /api/customer-auth/register` — registro comprador
- `POST /api/customer-auth/login` — login comprador
- `POST /api/stores/register` — onboarding de loja
- `GET /api/products/**` — catálogo público
- `GET /api/categories/**` — categorias públicas
- `GET /api/marketplace/products` — marketplace global

---

## 6. Contexto Público da Loja (Storefront)

Para endpoints públicos que precisam saber qual loja está sendo acessada, o projeto usa o `PublicStoreContextService`.

### Como funciona:

1. O frontend envia o header `X-Public-Store` com o subdomínio da loja (ex: `obraja`)
2. Ou o backend extrai o subdomínio do host da requisição
3. O service busca a loja pelo campo `subdomain`
4. Se não encontrar, retorna erro explícito

```java
// No controller de pedido público
Store store = publicStoreContextService.resolvePublicStore(request);
```

### Regra importante:

- Não existe fallback automático. Sem contexto = erro.
- O frontend **sempre** deve informar a loja explicitamente.

---

## 7. Padrão de Service

Os services seguem este padrão:

```java
@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    // Injeção por construtor (sem @Autowired)
    public ProductService(ProductRepository productRepository, CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    // Métodos públicos = operações de negócio
    public ProductResponseDTO create(ProductRequestDTO dto, Long storeId) { ... }
    public ProductResponseDTO update(Long id, ProductRequestDTO dto, Long storeId) { ... }
    public List<ProductResponseDTO> listByStore(Long storeId) { ... }

    // Métodos privados = helpers internos
    private void validateCategory(Long categoryId, Long storeId) { ... }
}
```

### Convenções:

- Injeção por **construtor** (sem `@Autowired`)
- Um service por domínio principal (`ProductService`, `OrderService`, `StockService`)
- Services podem chamar outros services quando necessário
- Transações são gerenciadas com `@Transactional` quando envolvem múltiplas escritas

---

## 8. Padrão de Repository

Interfaces simples que estendem `JpaRepository`:

```java
public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByStoreIdAndActiveTrue(Long storeId);

    Optional<Product> findByIdAndStoreId(Long id, Long storeId);

    boolean existsBySkuAndStoreId(String sku, Long storeId);
}
```

### Convenções:

- Sempre filtrar por `storeId` em queries de entidades segregadas
- Usar `Optional` para buscas por ID
- Nomear métodos seguindo a convenção Spring Data
- Para queries complexas, usar `@Query` com JPQL

---

## 9. Padrão de DTO

O projeto usa **Java Records** para DTOs:

```java
// Request (entrada)
public record ProductRequestDTO(
    @NotBlank String name,
    @NotNull @Positive BigDecimal price,
    @NotNull Integer stockQuantity,
    @NotBlank String unit,
    Long categoryId
) {}

// Response (saída)
public record ProductResponseDTO(
    Long id,
    String name,
    BigDecimal price,
    Integer stockQuantity,
    String unit,
    String categoryName,
    Boolean active,
    LocalDateTime createdAt
) {}
```

### Convenções de nomenclatura:

| Sufixo | Uso |
|--------|-----|
| `RequestDTO` | Entrada (body da requisição) |
| `ResponseDTO` | Saída (corpo da resposta) |
| `SummaryResponseDTO` | Saída resumida para listagens |
| `DetailResponseDTO` | Saída completa para tela de detalhe |

---

## 10. Configurações Importantes

### `application.properties`

```properties
# Banco de dados
spring.datasource.url=jdbc:postgresql://localhost:5433/nerva_db?sslmode=disable
spring.datasource.username=postgres
spring.datasource.password=postgres

# JPA — apenas valida schema (Flyway cuida das alterações)
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=true

# Flyway ativo
spring.flyway.enabled=true
spring.flyway.baseline-on-migrate=true

# JWT
app.jwt.secret=<segredo de 64 chars>
app.jwt.expiration-ms=86400000
```

### Pontos críticos:

- `ddl-auto=validate`: O Hibernate **nunca** altera o banco. Só valida.
- Se entity e schema não batem → erro na subida da aplicação.
- Toda alteração de schema vai em migration Flyway.

---

## 11. Fluxo de uma Requisição (Resumo Visual)

```
Request HTTP
    │
    ▼
[JwtAuthenticationFilter] → extrai token, identifica principal
    │
    ▼
[SecurityConfig] → verifica se rota é pública ou requer auth
    │
    ▼
[Controller] → valida DTO (@Valid), extrai user, chama service
    │
    ▼
[Service] → regras de negócio, validação, orquestração
    │
    ▼
[Repository] → acesso ao banco via Spring Data JPA
    │
    ▼
[Controller] → monta ResponseEntity com DTO de saída
    │
    ▼
Response HTTP
```

Se ocorrer erro em qualquer camada:

```
Exception lançada
    │
    ▼
[ApiExceptionHandler] → captura, formata resposta padronizada
    │
    ▼
Response HTTP com erro formatado
```

---

## 12. Checklist para Novas Features

Antes de abrir PR, verifique:

- [ ] Nova tabela/coluna? → Criou migration em `db/migration/`
- [ ] Novo endpoint? → Criou DTO de request e response (records)
- [ ] Validação? → Usou Bean Validation no DTO (`@NotBlank`, `@Positive`, etc.)
- [ ] Segurança? → Endpoint protegido usa `@AuthenticationPrincipal` e `AccessControlService`
- [ ] Multi-tenant? → Queries filtram por `storeId`
- [ ] Lógica? → Está no service, não no controller
- [ ] Entity exposta? → Nunca retornar entity direto, usar DTO
- [ ] Testes? → Pelo menos validar que a aplicação sobe sem erro (`mvnw.cmd test`)
