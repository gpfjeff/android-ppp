## General Questions ##

### What is Perfect Paper Passwords? ###

[Perfect Paper Passwords](https://www.grc.com/ppp) is a one-time password system developed by Steve Gibson of the [Gibson Research Corporation](https://www.grc.com/). It provides a relatively simple, safe, and secure form of multi-factor authentication that virtually anyone can implement. For additional information about PPP, please see the official PPP site at GRC.com.

### Why does this project exist? ###

Several PPP clients exist for other platforms, including other Java-enabled phones. Android is a powerful, growing smartphone platform that is easy to program for. The developers of PPP for Android would like to see a simple yet powerful PPP client for their favorite mobile platform.

### Can I use PPP for Android with my GMail/Yahoo!/Facebook/etc. account? Can I use it to store my existing passwords? Can it generate arbitrary passwords for me? ###

Unfortunately, no. PPP for Android is not a password vault or manager that stores arbitrary passwords. PPP is a multi-factor authentication system that requires both a client and server component. That means that the service you wish to authentication with must provide PPP as an option or requirement before you can use PPP for Android. Most major services like Google, Yahoo!, Facebook, etc. do not currently support PPP. However, many other services are adding PPP as a cheap method for increasing login security. If you are uncertain whether or not your service uses PPP, please contact the service's system administrator or customer support. If they do use PPP, you should be able to use PPP for Android to manage your passcodes, assuming they provide you with the parameters to recreate the passcode sequence.

### Why keep the card metaphor? Wouldn't it be easier to just specify the card, row, and column and get the passcode? ###

That's what some of the other clients do, yes, and we do this for small-sized screens that cannot display the full card practically. (See additional questions below regarding small-sized screens.) It's definitely a simpler interface and straight to the point, eliminating seemingly unnecessary data.

That said, we think that extra card data _is_ necessary. Viewing the entire card, complete with "strike-outs" of used passcodes, provides valuable history data on how the sequence is being used. By looking at which passcodes have and have not been stricken, you can, for example, see if there have been potential unauthorized login attempts for your ID. While the physical printed PPP cards may seem a bit outdated in this digital age, the data they provide can give insight into how the account is used. PPP for Android keeps this metaphor and all its advantages, at the same time eliminating the "paper".

### Is Steven Gibson involved with this project? ###

Aside from creating the original concept and writing the original specification, no. PPP for Android is built entirely from scratch or from code contributed to the community by other PPP enthusiasts. While we have not actively sought Mr. Gibson's blessing or input, we do plan to announce this project's existence to him before releasing it to the Android Market.

### What versions of the Perfect Paper Passwords specification is this app compatible with? ###

PPP for Android is compatible with version 3 of the PPP specification, meaning it should be usable with any PPP implementation. However, there may be practical limits on what size cards you can use with PPP for Android; see below for additional notes on these limitations.

## Card Set Maintenance ##

### Why can't I create a card set of any size? It keeps telling me some combinations of rows, columns, and passcode length make a card that is too big to display. ###

Unfortunately, screen real estate on the typical smartphone screen is quite limited. This is compounded by the fact that the built-in Android component that provides scrolling capabilities only works for vertical scrolling, not horizontal scrolling. While it is possible to change the font size to make the displayed passcodes smaller, this becomes impractical at some point when the codes are too small and too close together to be tapped individually.

Therefore, there is a practical constraint on how wide a PPP card can effectively be. Through a great deal of trial and error, we arrived at a decent combination of font size, padding, and other factors that made the default PPP card settings fit well in portrait orientation on the typical Android smartphone screen. We then developed a formula by which we estimate how wide different combinations of rows, columns, and passcode lengths will be and constrict any combination that we decide will be too wide. Unfortunately, this means that certain combinations of these values cannot be practically used.

### Can I edit a card set once it has been created? ###

PPP for Android allows the following editing options on an existing card set:
  * Rename (changes the display name in the card set list and the card view);
  * Clear strike-out data (either for an individual card or for the entire set);
  * Delete (the entire card set and all strike-out data).

If you feel you need to make more disruptive modifications to a card set, we recommend you create a new card set with the new parameters and delete the old card set if it is no longer needed. Adding new card sets is fairly simple and straightforward, so this should not be too much of a burden on the user.

### Why can't I change any of the other parameters? ###

Each card set parameter has unique reasons for why editing is not permitted:
  * The internal ID number uniquely identifies a card set in the database, even if the display name is changed. Modifying this ID would be disruptive to the entire database. Since this ID is never displayed to the user, it is largely ignorable and the user does not need to worry about it.
  * Changing the number of rows, columns, and/or the passcode length disrupts both the physical display of the card on the screen and the strike-out data within the database. If, for example, the number of rows changes, all strike-out data previously recorded becomes invalid. This data would need to be deleted to maintain the internal integrity of the database. Adjusting any of these three values can shift the structure of the card to such a point that existing strike-outs become useless.
  * Changing the alphabet or the sequence key fundamentally changes the passcodes on the cards. In effect, changing these values makes a new card set anyway, and we feel it would be simpler and create less confusion to go ahead and create a new card set instead of modifying the existing one.

### Can I see my card set parameters after they've been created? ###

Yes. In the initial card list, long-press the name of any card set to bring up a context menu for that set. Tap "Details" to display a read-only view of set's parameters.

### How do I delete a card set? ###

From the initial card list: Long-press the name of the card set to delete. This brings up a context menu for that set. Tap "Delete". You will be asked for confirmation before the card set will be deleted.

From the read-only card set details page: Tap the Menu hard button, then select "Delete". You will be asked for confirmation before the card set will be deleted.

## Card View ##

### How do I view my passcode cards? ###

From the initial card set list screen, tap the name of the card set. This default action takes most users to the card view, which displays the current or "last" card in the set. Note that devices with small-sized screens will get a different "single passcode" view instead; see additional questions below.

### How do I mark a passcode as used? ###

Simply tap on the passcode on the screen. Each passcode is really a "toggle button" that records the state of the passcode. When you tap a passcode for the first time, it "strikes through" the passcode, marking it with a red line. Tapping the passcode again clears the "strike" restoring the passcode to its original state. Each time the passcode is toggled, its state is recorded in a database so it will appear "stricken" if you leave the card and later come back to it.

As a convenience, "striking" a passcode by default also copies the text of the passcode to the system clipboard so it can be easily pasted into an authentication form. Clearing a "strike-out" does not copy the passcode to the clipboard. You can prevent PPP for Android from copying your passcodes in the application settings. (See below.)

### How do I move from card to card? ###

In the card view, you can move from card to card in one of three ways:
  * You may tap the Previous ("<<") and Next (">>") buttons at the top of the card to move one card backward or forward in the sequence respectively.
  * If you swipe your finger left to right across the card, you will move backward one card to the previous card. If you swipe your finger right to left, you will move forward one card to the next card.
  * You may jump to any card in the sequence by tapping the Menu hard button and selecting "Go To". A dialog box will appear, where you can enter any integer card number between one and the maximum size (2,147,483,647).

Note that none of these options will allow you to move beyond the first and final cards in the sequence. For example, if you are on the first card, the Previous button and the previous swipe gesture will be disabled. Attempting to select a card number in the "Go To" dialog outside the valid range will produce an error.

PPP for Android remembers the last card you viewed when you leave the card view. When you return to the card view later, this last or "current" card will be restored so you will know where you left off.

### Why do my cards only display in landscape orientation? ###

See the question above concerning card sizes. In addition to setting a maximum card width, we found we also needed a constraint on how wide a card can be to fit on the screen in portrait orientation. If the card is below this constraint, it can easily be displayed in either portrait or landscape mode and it will rotate accordingly as the device is rotated. If, however, the card is deemed too wide to fit in portrait mode, PPP for Android will force the card to be displayed in landscape mode only. This will not affect the functionality of the card view in any way; it will work perfectly fine in this mode, although you may need to scroll vertically to see the entire card.

## "Single Passcode" View ##

### I don't get a "card view" when I tap on a card set name. I get a screen that seems to only show one passcode at a time. ###

It appears you have a device with a "small" screen, or at least that's what Google classifies it as. The Android SDK lists a number of generalized screen sizes, which it designates as "small", "normal", "large", and "xlarge". Our code detects "small" sized screens and displays an alternate "single passcode" view rather than the full card view described above.

In our tests, we discovered that in order to fit the entire default card size on the screen at once, the text size becomes so small on these small-screen devices that it becomes virtually unreadable and nearly impossible to toggle passcodes with any form of accuracy. Our initial thought was to filter out such devices to prevent PPP for Android from being used on them, but we didn't want to leave _any_ potential user out if we didn't have to. So we created this alternate view as a less than optimal workaround.

### How does the "single passcode" view work? ###

Once you select a card set and the "single passcode" view is displayed, you will see four main UI elements. At the top, you will see a text box labeled "Card Number" and two drop-down lists or "spinners" labeled "Column" and "Row". With these three controls you can navigate to any individual passcode within the card set sequence. Changing any of these values immediately navigates to the specified passcode.

Beneath these controls you will see the passcode displayed in large text. Like the card view described above, this is actually a "toggle button" and works in exactly the same manner. Tapping the passcode will "strike through" it, and that "strike" will be recorded in the database. As you navigate through passcodes, the "strike-out" status will be restored and displayed, just like in the full card view. Tapping the passcode while "stricken" will clear the "strike". If the appropriate settings has been set, "striking" a passcode will also copy its value to the system clipboard.

### Why does the "single passcode" view initially display a different passcode than the last one I used? ###

Currently, the system doesn't remember the last individual passcode displayed. In the card view, this is unnecessary; it only needs the last card displayed, and the individual passcodes are "stricken" based on the data stored in the database.

As a workaround, when the "single passcode" view loads it first goes to the last card displayed. It then queries the "strike-out" database table to find which passcode on the card was the last to be "stricken". If nothing is returned, the view defaults to the first passcode on the card. If it finds something, the view then increments the column, then row, then card number if necessary to move to the next passcode in the sequence. Assuming that you strike-out each passcode after use and no one else has attempted to access the account you are authenticating with, this should be the same passcode as what the site is looking for.

This algorithm isn't perfect, of course, and several factors could cause the calculated passcode to be the wrong one that you need. In that case, simply change the card, column, and/or row numbers as appropriate to get the correct passcode.

## Settings and Options ##

### Can I protect my passcodes with a password? ###

Yes. From the initial card list, tap the Menu hard button, then select Settings. There you will find a big button marked "Set Password". Tap this button to launch a dialog box where you can enter your desired password. You will be required to enter your password twice. After the password is set, any subsequent access of the program will prompt you for your password before you can access your passcodes.

You can later remove your password by repeating this process; instead of "Set Password", the button will display "Clear Password". You will be required to enter your password as confirmation before it is completely removed. You can change your password at any time by first clearing the old one, then setting a new one.

Please note that your password will be stored in the system as a one-way cryptographic hash, meaning it cannot be recovered if it is lost. If you forget your password, the only way you will be able to get back into PPP for Android will be to delete all data in the database. This can be done from the Android system settings, but it is easier to access from the PPP for Android password prompt screen. Tap the Menu hard button, then select "Clear Password". You will be asked for confirmation before your data will be deleted.

### Does setting a password encrypt my card set data? ###

Partially. When the master password is set, all card set sequence keys in the database will be encrypted using AES-256 keyed off the master password and salted using a randomly generated salt. The sequence key will be decrypted on the fly whenever the card set details or the current card are displayed. This encryption is transparent to the user; the app will operate in an identical fashion whether the sequence key is encrypted or not. If the master password is cleared through the settings activity, all sequence keys will be subsequently decrypted and stored in the clear as before.

While there are protections built into Android to prevent other applications or the user from directly accessing databases outside the application that created them, there _are_ means of bypassing these protections, such as "rooting" the device. Therefore, encrypting at least part of the card set data provides an additional layer of protection for the user's data. After careful consideration, we determined that the most critical part of the card set is the sequence key, which drives the PPP engine and makes each card set unique. The remaining metadata (rows, columns, alphabet, etc.) are also sensitive, but they will be useless without the unique sequence key to drive them. Thus, as a trade-off between security and performance, we elected to encrypt just the sequence key and leave the remaining parameters in the clear.

### Can I stop PPP for Android from copying passcodes to the clipboard? ###

Yes. From the initial card list, tap the Menu hard button, then select Settings. Uncheck the "Copy Passcodes to Clipboard" check box. This change takes effect immediately and striking out any subsequent passcode will no longer copy the passcode to the clipboard. If you'd like to re-enable this feature, simply repeat this process.

## Platform Questions ##

### What versions of Android will PPP run on? ###

We want to make PPP for Android as accessible as possible. Therefore, we are targeting a minimum version of Android 1.1 (API level 2). Because Android has built-in forward compatibility, this means it will run on every version of the platform after Android 1.1, although some versions may offer a less optimal user experience.

### Will PPP for Android run on Android tablets (i.e. "Honeycomb" (3.0) or later)? ###

Yes. See the question above; Android apps are inherently forward compatible, so PPP for Android will run on "Honeycomb" and later tablets. We are also working on a dedicated "extra large" layout to accommodate the tablet form factor. While it may not make the most efficient use of a tablet's screen real estate, PPP for Android should look like a native 3.0+ app.

### What special permissions will PPP for Android require to run? ###

Currently, PPP for Android does not require any special permissions. It does not access the Internet, it does not access the SD card or other forms of external storage, and it does not require access to your mail, contacts, phone features, camera, or any other specialized hardware. Like Mr. Gibson, we believe in a "trust no one" mentality, and we don't believe an application like PPP for Android should be snooping around where it does not belong. Since none of these features are required, they are not requested.

Note, however, that if we implement some of the features listed on the FutureFeatures page, this policy may change. For example, adding the ability to import or export card set data may require adding permission to write to external storage (i.e. the SD card). We will announce these changes if and when they become relevant.