//
//  GameServicesHelper.h
//  gameservices
//
//  Created by Stuart Crook on 08/02/2023.
//

#import <UIKit/UIKit.h>
#import <GameKit/GameKit.h>

#import "gameservices.h"

NS_ASSUME_NONNULL_BEGIN

@interface GameServicesHelper : NSObject <GKGameCenterControllerDelegate>

@property GameServices *services;
@property NSString *currentLeaderboardID;

- (id)initWithGameServices:(GameServices *)services;
- (void)presentViewController:(UIViewController *)viewController;
- (void)presentViewController:(UIViewController *)viewController forLeaderboardID:(NSString *)leaderboardID;

@end

NS_ASSUME_NONNULL_END
