# Política de segurança

[English](SECURITY.md)

## Versões suportadas

Ainda não há versão lançada. A política de suporte será definida antes da primeira release pública.

## Como comunicar uma vulnerabilidade

Não publique vulnerabilidades em issues públicas. Até que exista um canal privado, contate os mantenedores pelo contato privado indicado no repositório de release. Informe impacto, passos de reprodução, versão/commit afetado e possível mitigação, sem credenciais ou dados privados.

## Postura planejada

O StackEmu é planejado somente para desenvolvimento local confiável. O Compose de referência prende a GUI ao loopback. A aplicação não deve montar o socket Docker, rodar como root, executar shell arbitrário, registrar segredos nem exibi-los em HTML/erros. Mutações exigem CSRF; dados de objeto são escapados e downloads usam cabeçalhos defensivos. Esses são requisitos, não controles já verificados.
