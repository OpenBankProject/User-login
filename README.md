Bank-account-management-api
==========================

An application that provides API calls to manage bank account credentials, writes them into a message queue ('management') and waits for an acknowledgement that the calls were processed. Before writing into the queue, the pin code will be signed with a public key.
The application supports oAuth-authentication, the credentials for this can be received by the OBP-API application ( https://github.com/OpenBankProject/OBP-API ).

The configuration in default.props.template should be changed to the following:
publicKeyPath= path to the public key file used for the pin code (e.g.: /home/usr/...)
For connection to the message queue use the default settings of RabbitMQ:
connection.user=guest
connection.password=guest