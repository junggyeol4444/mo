package com.junggyeol.mininggacha;

import com.junggyeol.mininggacha.player.PlayerPoints;

import java.util.Random;

/**
 * 뽑기 로직 (서버에서만 호출)
 */
public class GachaService {
    private final Random random = new Random();

    /**
     * 시도. true면 퇴근 허용(true)로 바꿈. 포인트 부족이면 IllegalStateException을 던짐.
     */
    public boolean tryGacha(PlayerPoints pp) {
        int cost = MiningGachaMod.CONFIG.gachaCost;
        if (!pp.consumePoints(cost)) throw new IllegalStateException("Not enough points");
        double r = random.nextDouble();
        boolean success = r < MiningGachaMod.CONFIG.quitChance;
        if (success) pp.setAllowedToQuit(true);
        return success;
    }
}