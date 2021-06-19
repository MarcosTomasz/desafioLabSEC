# desafioLabSEC
Este Web-app desenvolvido para o desafio para a vaga de Dev Backend.
O código está separado em:
- front-end
- backend

## backend:
O `backend` da aplicação é escrito em Java 8 com o projeto configurado pelo SpringBoot, sendo executado na máquina local, porta 8080.

## front-end
O `front-end` da aplicação usa o `framework` `React` para fazer requisições ao `backend` e renderizar seu resultado, para o estilo da página foi utilizado o `framework` `bootsrap` para marcações de estilo, sendo executado na máquina local, porta 3000.

## Instalação
Para executar este projeto você precisa ter instalado `java-8-openjdk`, `git`, `npm` e `gradle`.
Após os seguintes comandos:

```
git clone https://github.com/MarcosTomasz/desafioLabSEC.git
cd desafioLabSEC
cd front-end
npm install
npm start
```
Em outro terminal:
```
cd desafioLabSEC
cd back-end
./mvnw install
./mvnw run
java -jar target/desafio-0.0.1-SNAPSHOT.jar

```

PS: `npm start` deve permanecer rodando juntamente ao `java -jar target/desafio-0.0.1-SNAPSHOT.jar`

## Uso
Para usar o web-app acesse http://localhost:3000/ 
