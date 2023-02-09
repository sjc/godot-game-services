//
//  admob_module.mm
//  admob_module
//
//  Created by Gustavo Maciel on 16/01/21.
//

#import "gameservices_module.h"
#import "core/engine.h"

GameServices * gameservices;

void register_gameservices_types() {
    NSLog(@"init gameservices plugin");

    gameservices = memnew(GameServices);
    Engine::get_singleton()->add_singleton(Engine::Singleton("GameServices", gameservices));
}

void unregister_gameservices_types() {
    NSLog(@"deinit gameservices plugin");
    
    if (gameservices) {
       memdelete(gameservices);
   }
}
