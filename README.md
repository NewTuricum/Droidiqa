# Droidiqa - A Zilliqa library for Android

Droidiqa is an Android wrapper around Zilliqa's firestack library and comes with several improvements:
- Resolves SpongyCastle key generation issues on higher Android SDKs
- Maintains its own room database
- Lifecycle awareness
- Observable LiveData interface to Zilliqa blockchain
- Secure key storage
- Contacts database

# Current state of the library

Right now I consider this library to be in a beta-phase and by no means ready or even perfect. This is strictly a hobby project of mine, please keep this in mind as you're using Droidiqa. **Use this code at your own risk!**
There is still a lot of streamlining and optimizing work to do here. I'm also aware of the issues all those co-dependencies might cause, especially AndroidX, Volley and Room. Right now I'm planning on getting rid of those via some form of interface, but this may take some more thinking on my part. As of now, if you have any feedback or want to contribute in the development of this project, please feel free to drop me a few lines at contact@newturicumworks.ch

# How to use Droidiqa in your app  
## Step 1: Library dependency
Add Droidiqa dependency to your gradle build script:

    implementation 'ch.newturicum.libraries:droidiqa:<version>'
## Step 2: Initialize Droidiqa singleton  
Since Droidiqa is lifecycle aware, extend your application class to implement both the AndroidX LifecycleObserver and DroidiqaLifecycleProvider interfaces as such:

    class MyApplication : Application(), LifecycleObserver, DroidiqaLifecycleProvider {
	    override fun onCreate() {
		    super.onCreate()
		    ProcessLifecycleOwner.get().lifecycle.addObserver(this)
	    }
	    override fun getProcessLifecycleOwner() : LifecycleOwner = ProcessLifecycleOwner.get()
    }

Next, obtain a Droidiqa instance from your application class object:

    val droidiqa = Droidiqa(myAppInstance)
> Optional: You may provide your own key encoder and / or Volley request queue here.

**Note: Ensure to make this instance of droidiqa a process-wide singleton!**

That's it! You are now ready to use Droidiqa and access the Zilliqa blockchain.

# Usage examples
### Setting active network

By default Droidiqa maintains a separate wallet for both the test- and main-networks of Zilliqa. These can easily be switched:

    droidiqa.setNetwork(ZilNetwork.TEST)
Will set the active network and wallet to run on Zilliqa's test network for instance.
>CAUTION: By default Droidiqa will run on the MAIN network.

### Import an existing account
Known accounts can be added to the active wallet by providing their private keys:

    val existingAccount = droidiqa.addAccount(<privateKey>, "My old account")

### Creating a new Zilliqa account
Adding a new account to the currently active wallet is just as easy:

    val newAccount = droidiqa.newAccount("My new account")

### Observing data
Droidiqa provides all its data via several observable LiveData objects. For example, observe any change within the current wallet via:

    droidiqa.observables.walletLiveData.observe(viewLifecycleOwner, { wallet ->
       textViewBalance.text = "Current balance: ${wallet.activeAccount?.zilBalance} Zil"
    }

### Handling ZRC2-Tokens
Each wallet contains a bucket of tokens that can be added or removed easily. For example in order to add an as of now unknown token via its address:

    droidiqa.addToken(<TokenAddress>, callback: AddTokenCallback?)
Droidiqa's observables object also contains a LiveData field that let's you listen for any changes made to any of the currently known tokens.

### Transferring ZRC2-tokens
In order to send ZRC2-tokens, you must call the smart contract transition interface with a pre-filled ZRC2TokenTransfer-object, such as this:

    val tokenTransferTransition = ZRC2TokenTransfer(<receiverAddress>, <amount>)
And then pass this transition object into droidiqa's smart contract caller:

    droidiqa.callSmartContractTransition(<tokenContractAddress>, <gasPrice>, tokenTransferTransition, callback: TransitionCallback?)

### Transferring Zilliqa tokens
For a simple Zilliqa-token transfer you may use the convenience method sendZilliqa:

    droidiqa.sendZilliqa(<amount>, <receiverAddress>, <gasPrice>, callback: TransitionCallback?)
This will transfer the given amount of Zilliqa tokens from the wallet's active account to the receiver address.

### Smart contract transitions
As discussed above under "transferring ZRC2-tokens", you must provide a Transition-object to Droidiqa's callSmartContractTransition-method. The Transition-class is open and can be extended as necessary. It also provides convenience methods to handle transition parameters.

# License
This library is released for free use under the GNU General Public License, v3
