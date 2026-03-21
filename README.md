# Bahia Brindes — Backend

O Bahia Brindes é um sistema integrado feito sob medida para pequenas empresas do ramo de brindes e presentes personalizados. Ele nasceu de um problema real: no dia a dia dessas empresas, o fluxo de pedidos costuma ser desorganizado, o estoque é controlado em planilhas (quando é controlado), e a comunicação entre vendas, produção e compras acaba gerando muito retrabalho.

A proposta é reunir tudo isso em um único lugar.

## O que o sistema faz?

Do lado do **cliente**, ele pode navegar pelo catálogo de produtos, montar um carrinho e solicitar um orçamento. Depois, acompanha o andamento do pedido — desde a aprovação da arte até a entrega — tudo pela própria plataforma.

Do lado da **empresa**, os funcionários têm acesso a um portal interno com módulos para:

- **Estoque** — registrar entrada e saída de matéria-prima, acompanhar níveis e receber alertas quando algo está acabando.
- **Produtos** — cadastrar itens com ficha técnica, imagens, preço de custo e venda.
- **Vendas e Clientes** — gerenciar clientes, criar orçamentos, acompanhar status e converter em pedidos.
- **Cadastros auxiliares** — categorias, fornecedores, locais de estoque, unidades de medida.
- **Administração** — usuários, perfis e permissões de acesso.

O administrador tem controle total sobre quem pode acessar o quê.

## Como funciona a autenticação?

O sistema trabalha com três perfis de acesso:

| Perfil | O que pode fazer |
|--------|-----------------|
| **Cliente** | Ver catálogo, solicitar orçamentos, acompanhar pedidos, editar seu perfil |
| **Funcionário** | Tudo do portal interno: estoque, produtos, vendas, clientes |
| **Admin** | Tudo acima + criar funcionários e gerenciar permissões |

A autenticação é feita com JWT (token), então não há sessão no servidor — cada requisição carrega o token de acesso.

## Fluxo de um orçamento

```
Cliente solicita → ORÇAMENTO SOLICITADO → ARTE PENDENTE → EM PRODUÇÃO → CONCLUÍDO
```

O histórico de cada mudança de status fica registrado, junto com comentários e artes anexadas.

---

## 🔧 Detalhes técnicos

### Stack

| Tecnologia | Versão | Uso |
|-----------|--------|-----|
| Java | 17 | Linguagem principal |
| Spring Boot | 3.3.5 | Framework web + DI |
| Spring Data JPA | — | Acesso a dados |
| Spring Security | — | Autenticação e autorização |
| PostgreSQL | — | Banco de dados |
| JWT (jjwt) | 0.11.5 | Tokens de autenticação |
| Lombok | 1.18.34 | Redução de boilerplate |
| Swagger (springdoc) | 2.1.0 | Documentação da API |
| Freemarker | 2.3.31 | Templates de email |
| BCrypt | — | Hash de senhas |

### Estrutura do projeto

```
src/main/java/br/ed/ufape/bahiabrindes/
├── config/          # Segurança, CORS, Swagger, dados iniciais
├── controller/      # 11 controllers REST
├── dto/             # Objetos de transferência, organizados por módulo
├── exception/       # Tratamento global de erros
├── model/
│   ├── entity/      # 19 entidades JPA
│   └── enums/       # StatusOrcamento, StatusArte, TipoUsuario
├── repository/      # 19 repositórios Spring Data
├── security/        # Filtro JWT e utilitário de token
└── service/         # 12 serviços com a lógica de negócio
```

### Principais endpoints

| Recurso | Base | Observações |
|---------|------|-------------|
| Autenticação | `/api/auth` | Login, cadastro, recuperação de senha |
| Orçamentos | `/api/orcamentos` | CRUD + admin + mudança de status |
| Produtos | `/api/produtos` | GET público, demais protegidos |
| Estoque | `/api/estoque` | Resumo, itens, movimentações |
| Matérias-primas | `/api/estoque/materias-primas` | CRUD completo |
| Fornecedores | `/api/estoque/fornecedores` | CRUD completo |
| Clientes | `/api/clientes` | POST público, `/me` para o próprio cliente |
| Funcionários | `/api/funcionarios` | Criação restrita a admin |

A documentação completa da API está disponível via Swagger em `/swagger-ui/index.html` quando o servidor estiver rodando.

### Como rodar localmente

**Pré-requisitos:** Java 17+, PostgreSQL rodando com um banco criado.

1. Configure as variáveis de ambiente ou o `application.properties` com os dados do banco.
2. Execute:

```bash
./mvnw spring-boot:run
```

O servidor sobe em `http://localhost:8080`.



Desenvolvido como projeto da disciplina de Projetão — UFAPE, 7º período.
