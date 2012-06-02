package i4nc4mp.myLock;

//the call screen is used as an optional feature so that we can replace the call prompt lockscreen

/*
 *  method for accepting call
 *  
   Intent new_intent = new Intent(Intent.ACTION_MEDIA_BUTTON);
 
    new_intent.putExtra(Intent.EXTRA_KEY_EVENT, new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_HEADSETHOOK));
   context.sendOrderedBroadcast(new_intent, null);
   
   Thanks AutoAnswer app which simply lets you set auto answer, a missing feature that most phones have
   *
   */

//what we will do with this is provide an onscreen button
//and also give user the choice if they would like side keys as answer buttons
