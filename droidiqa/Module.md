# Module droidiqa

Droidiqa is an unofficial Android wrapper around Firestack's Laksa Zilliqa-library and comes with
several OS-specific features:

- 🔧 Resolves SpongyCastle key generation issues on higher Android SDKs
- 💾 Maintains its own Room database
- 🔄 Lifecycle awareness
- 📊 Observable LiveData interface to Zilliqa blockchain
- 🔐 Secure key storage
- 📇 Contacts database

## ⚠️ Current state of the library - Warning

Right now I consider this library to be in a **beta-phase** and by no means ready or even perfect.
This is strictly a hobby project of mine, please keep this in mind as you're using Droidiqa.

> **Use this code at your own risk!**

There is still a lot of streamlining and optimizing work to do here. I'm also aware of the issues
all those co-dependencies might cause, especially AndroidX, Volley and Room. Right now I'm planning
on getting rid of those via some form of interface, but this may take some more thinking on my part.

## 🚀 Getting started

### Step 1: Library dependency

Add Droidiqa dependency to your gradle build script:

```
TBD
``` 

### Step 2: Initialize Droidiqa singleton

Since Droidiqa is lifecycle aware, extend your application class to implement both the AndroidX
`LifecycleObserver` and `DroidiqaLifecycleProvider` interfaces:

```kotlin
class MyApplication : Application(), LifecycleObserver, DroidiqaLifecycleProvider {
    override fun onCreate() {
        super.onCreate()
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun getProcessLifecycleOwner(): LifecycleOwner =
        ProcessLifecycleOwner.get()
}
``` 

Next, obtain a Droidiqa instance from your application class object:

```kotlin
val droidiqa = Droidiqa(myAppInstance)
``` 

> 💡 **Optional:** You may provide your own key encoder and/or Volley request queue here.

> ⚠️ **Important:** Ensure to make this instance of droidiqa a process-wide singleton!

That's it! You are now ready to use Droidiqa and access the Zilliqa blockchain.

## 💻 Usage examples

### Setting active network

By default Droidiqa maintains a separate wallet for both the test- and main-networks of Zilliqa.
These can easily be switched:

```kotlin
 droidiqa.setNetwork(ZilNetwork.TEST)
``` 

This will set the active network and wallet to run on Zilliqa's test network for instance.

> ⚠️ **CAUTION:** By default Droidiqa will run on the **MAIN** network.

### Import an existing account

Known accounts can be added to the active wallet by providing their private keys:

```kotlin
 val existingAccount = droidiqa.addAccount(privateKey = "key")
``` 

### Creating a new Zilliqa account

Adding a new account to the currently active wallet is just as easy:

```kotlin
 val newAccount = droidiqa.newAccount("My new account")
``` 

### Observing data

Droidiqa provides all its data via several observable LiveData objects. For example, observe any
change within the current wallet:

```kotlin
 droidiqa.observables.walletLiveData.observe(viewLifecycleOwner) { wallet ->
    textViewBalance.text = "Current balance: ${wallet.activeAccount?.zilBalance} ZIL"
}
``` 

### Handling ZRC2-Tokens

Each wallet contains a bucket of tokens that can be added or removed easily. For example, to add an
unknown token via its address:

```kotlin
 droidiqa.addToken(tokenAddress =

``` 

Droidiqa's observables object also contains a LiveData field that lets you listen for any changes
made to any of the currently known tokens.

### Transferring ZRC2-tokens

In order to send ZRC2-tokens, you must call the smart contract transition interface with a
pre-filled `ZRC2TokenTransfer` object:

```kotlin
 val tokenTransferTransition = ZRC2TokenTransfer(receiverAddress = "addr")

``` 

Then pass this transition object into droidiqa's smart contract caller:

```kotlin
 droidiqa.callSmartContractTransition(contractAddress = "addr")

``` 

### Transferring Zilliqa tokens

For a simple Zilliqa-token transfer you may use the convenience method `sendZilliqa`:

```kotlin
 droidiqa.sendZilliqa(amount = 123)

``` 

This will transfer the given amount of Zilliqa tokens from the wallet's active account to the
receiver address.

### Smart contract transitions

As discussed above under "transferring ZRC2-tokens", you must provide a `Transition` object to
Droidiqa's `callSmartContractTransition` method. The `Transition` class is open and can be extended
as necessary. It also provides convenience methods to handle transition parameters.

```kotlin
class CustomTransition : Transition() {
    init {
        // Add your custom parameters
        addParam("param1", "value1")
        addParam("param2", 123)
    }
}

val customTransition = CustomTransition()
droidiqa.callSmartContractTransition(
    contractAddress = "zil1contract...",
    gasPrice = "2000000000",
    transition = customTransition,
    callback = transitionCallback
)
```

## 🙏 Acknowledgements

This project is based on [Firestack's Laksa library](https://github.com/FireStack-Lab/Laksa).

## 📄 License

This library is released for free use under the . **GNU General Public License, v3**
