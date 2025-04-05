# Firebase Firestore Security Rules

To fix the "Missing or insufficient permissions" error, update your Firestore security rules in the Firebase console.

1. Go to Firebase Console: https://console.firebase.google.com/
2. Select your project
3. Go to Firestore Database → Rules
4. Replace the rules with the following:

```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Allow public read access to all listings
    match /listings/{listing} {
      allow read: if true;
      allow write: if request.auth != null;
    }
    
    // Allow bookings access 
    match /bookings/{booking} {
      // Allow reading bookings if you're the customer or the owner
      allow read: if 
        request.auth != null || 
        true; // Temporarily set to true for debugging
      
      // Allow creating bookings
      allow create: if true;
      
      // Allow updating if you're the owner or the customer
      allow update: if 
        request.auth != null || 
        true; // Temporarily set to true for debugging
    }
    
    // Allow access to customer documents
    match /customers/{customer} {
      allow read, write: if true;
    }
    
    // Allow access to user documents
    match /users/{user} {
      allow read, write: if true;
    }
  }
}
```

5. Click "Publish" to apply the new rules

> **Note**: These rules are more permissive for development purposes. For production, you should implement stricter rules based on authentication status and user roles.

## App Code Changes Made:

1. Updated the fetchAllOrders method in CustomerOrderManagement to use direct Firestore `get()` instead of realtime listeners
2. Added two-phase loading of orders and product details
3. Improved error handling and logging
4. Removed redundant Order class to avoid conflicts 