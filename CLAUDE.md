# Convencoes do projeto

Contexto para o Claude Code ao trabalhar neste repositorio.

## Arquitetura

- **Pacote por feature**, nao por camada. `account/`, `transaction/`, `idempotency/`.
  Nada de `controllers/`, `services/`, `repositories/` no topo: quem abre o projeto
  precisa ver o dominio, nao o framework.
- **Modelo de dominio rico.** Regra de negocio dentro da entidade. Service orquestra,
  nao decide. Se aparecer um setter publico numa entidade, algo esta errado.
- **Agregados isolados.** Referencia entre agregados e por UUID, nunca por `@ManyToOne`.

## Regras nao negociaveis

- `BigDecimal` para dinheiro. `double` e `float` sao proibidos no dominio.
- Schema so muda via migration Flyway. `ddl-auto` permanece em `validate`.
- Toda operacao que causa efeito colateral exige `Idempotency-Key`.
- Entidades JPA nao vazam para o controller. Sempre `record` de request/response.
- Erros seguem RFC 7807 via `ProblemDetail`.

## Testes

- Fluxos criticos sao cobertos por **teste de integracao com Testcontainers**,
  nao por mock. Mock de repositorio prova que o mock funciona.
- Todo teste novo de regra financeira precisa verificar o **saldo final**,
  nao apenas o status HTTP.

## Estilo

- Construtor para injecao de dependencia. Nunca `@Autowired` em campo.
- Sem Lombok neste projeto: o codigo gerado fica explicito.
- Comentario explica **por que**, nunca **o que**.
