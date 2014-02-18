### About

An application that allows the user to first login either by using its Open Bank Project account or a third party service.
And then adding its banking credentials to register a bank account.

The application encrypts the pin code using a public key, then writes into a message queue and waits for an acknowledgement (the message have been processed by the other application). It also provides two API calls to give both a request token and a access token, more details [here](https://github.com/OpenBankProject/OBP-API/wiki/OAuth-1.0-Server) and [here](https://github.com/OpenBankProject/OBP-API/wiki/Add-a-bank-Account).

### Props files
The configuration in default.props.template should be changed to the following:

* publicKeyPath= /PATH/TO/THE/PUBLIC/KEY
* For connection to the message queue use the default settings of RabbitMQ:  
connection.host=HOST  
connection.user=USER  
connection.password=PASSWORD
* Database  
db.driver=DB-DRIVER (example: org.postgresql.Driver)  
db.url=DB-URL (example: jdbc:postgresql://localhost:5432/database?user=foo&password=bar
* Hostname: under which address an port the application is running  
hostname=http://HOST:PORT (example: https://localhost:8080)