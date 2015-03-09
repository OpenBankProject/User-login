### About

An application that allows the user to first login either by using its Open Bank Project account or a third party service.
And then adding its banking credentials to register a bank account.

The application encrypts the pin code using a public key, then writes into a message queue and waits for an acknowledgement (the message have been processed by the other application). It also provides two API calls to give both a request token and a access token, more details [here](https://github.com/OpenBankProject/OBP-API/wiki/OAuth-1.0-Server) and [here](https://github.com/OpenBankProject/OBP-API/wiki/Add-a-bank-Account).

### Props files / Configuraiton
See sample.props.template.