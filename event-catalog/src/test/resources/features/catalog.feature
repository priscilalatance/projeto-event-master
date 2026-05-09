Feature: Catalogo de Eventos
  Como um organizador de eventos
  Eu quero cadastrar e buscar eventos
  Para que os clientes possam visualizar os eventos disponiveis

  Scenario: Criar evento com sucesso
    When o organizador cadastra o evento "Hamlet" com 500 ingressos a 120.0
    Then o evento deve ser criado com status "PUBLISHED"
    And o evento deve ter 500 ingressos disponiveis

  Scenario: Buscar evento por id
    Given um evento "O Fantasma da Opera" ja cadastrado
    When o cliente busca o evento pelo id
    Then o evento deve ser retornado com nome "O Fantasma da Opera"

  Scenario: Listar eventos por categoria
    Given eventos cadastrados nas categorias "TEATRO" e "MUSICAL"
    When o cliente filtra por categoria "TEATRO"
    Then somente eventos de "TEATRO" devem ser retornados
