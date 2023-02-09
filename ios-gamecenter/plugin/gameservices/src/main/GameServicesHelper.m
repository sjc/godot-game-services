//
//  GameServicesHelper.m
//  gameservices
//
//  Created by Stuart Crook on 08/02/2023.
//

#import "GameServicesHelper.h"

@implementation GameServicesHelper

- (id)initWithGameServices:(GameServices *)services {
    if (self = [super init]) {
        _services = services;
    }
    return self;
}

- (void)presentViewController:(UIViewController *)viewController {
    [[[[UIApplication sharedApplication] keyWindow] rootViewController] presentViewController:viewController animated:YES completion:nil];
}

- (void)presentViewController:(UIViewController *)viewController forLeaderboardID:(NSString *)leaderboardID {
    self.currentLeaderboardID = leaderboardID;
    [self presentViewController:viewController];
}

- (void)gameCenterViewControllerDidFinish:(nonnull GKGameCenterViewController *)gameCenterViewController {
    [gameCenterViewController dismissViewControllerAnimated:YES completion:^{
        self.services->emit_signal("show_leaderboard_dismissed", self.currentLeaderboardID.UTF8String);
        self.currentLeaderboardID = nil;
    }];
}

@end
