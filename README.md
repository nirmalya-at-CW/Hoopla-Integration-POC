### A Prototype to implement integration of 1Huddle Players' scores with the Leaderboard maintained by Hoopla.


Hoopla provides a number of ways for applications to connect to Hoopla's service. A bunch of publicly
available APIs is one of them. 1Huddle proposes to use these APIs to post Players' scores to Hoopla,
as and when they finish playing games and their scores are computed.

Hoopla uses OAuth for applications to authorize themselves to post scores. 

For the purpose of this prototype, I have created a Developer login, using the official email, 
namely nirmalya.s@codewalla.com. This trial login period expires in 15 days.

Using this login, I have created a Leaderboard named **Tic-Tac-Toe-Leaderboard** on Hoopla. To this, I have
added 3 fellow players, namely vikas.s@codewalla.com, mayur.p@codewallla.com and shivali.b@codewalla.com. When we
run this prototype program, we simulate the cases where each of them plays a game, and their new scores are
posted to Hoopla. Hoopla, then rearranges its Leaderboard page. If we refresh the page, the new standing of 
the players, appears. While creating the Leaderboard, Hoopla requires us to provide a name of the field, whose
value determines the rank of players on the Leaderboard. I have named it as 'Score'. This name is important.

### Components

* com.huddle.integration.hoopla.HooplaConnector

This is the driver program. I have hardcoded the HOOPLA_CLIENT_KEY and HOOPLA_CLIENT_SECRET in this. Whenever someone
creates a developer login with Hoopla, these two values are provided. 

* com.huddle.integration.hoopla.HooplaExchangeActor

This is the actual workhorse of the prototype. This Actor interacts with Hoopla Service, using REST
APIs specified in Hoopla's [site](https://developer.hoopla.net/docs/authentication). It uses a simple HTTP
REST Client library called [Unirest](http://unirest.io/java.html).

### Workflow

HooplaConnector uses Client_Key/Client_Secret pair to obtain a session key. It uses this session key to create
an HooplaExchangeActor.

HooplaExchangeActor prepares itself by making a series of calls to Hoopla Service, in the following order:
*   It gets a list of Users (players) from Hoopla. Every User is identified by a unique ResourceID in Hoopla.
The 1Huddle application, however, knows every player by his/her emailID. A map is created which contains 
User-specific resource returned by Hoopla, against the User's email ID.
*   It retrieves a unique ResourceID for the field determining the position of players on the Leaderboard (in our
case, _Score_). 
*   It retrieves an array of unique ResourceIDs for every player's _Score_. It is important to understand that
every Player has his/her own unique Score's ResourceID. This is required to update a particular player's score.

Then the HooplaExchangeActor is ready.

HooplaConnector then sends a series of messages (_tells_, to be precise) to HooplaExchangeActor, asking to update a Player's
score. The message is of the form:
`exchange ! UpdateScore("vikas.s@codewalla.com", 11)`
The series simulates the situation when each player finishes a GameSession, and his/her final Score is posted to 
Hoopla.

The `Sleep` calls are interspersed to let the Actor complete asynchronous HTTP REST calls to Hoopla. In the actual
implementation, the Actor will take precautions so that this precaution can be easily removed. The caller doesn't 
need to wait for the Actor to finish doing whatever it is doing. 

### Note

It is possible that despite a successful posting of score, Hoopla's Leaderboard page doesn't show the latest
standings, _immediately_. There indeed is a lag. We may have to refresh Hoopla's Leaderboard page a few times,
before the correct standing appears.

However, in the actual usage scenario, 1Huddle is not concerned about this delay. This is a behaviour of Hoopla, that it and its customers
have to settle for.



