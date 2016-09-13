(ns vgap.predict)

(defn final-turn [game]
  (apply max (keys (:turns game))))

(defn final-placements [game]
  (let [last (final-turn game)
        all-slots (keys (get-in game [:planets last]))
        turns-from-last (reverse (sort (keys (:turns game))))]
    (sort
      (fn [slot1 slot2]
        (let [all-turn-comparisons (map #(compare
                                           (get-in game [:planets % slot2])
                                           (get-in game [:planets % slot1]))
                                     turns-from-last)]
          (or (first (drop-while zero? all-turn-comparisons))
              0)))
      all-slots)))
  

(defn raw-tendency
  "Unnormalized odds in game g that player in slot on turn-number can achieve place (0 for 1st, 1 for 2nd, etc).
  As rough approximation, assume twice as many planets means double the odds of 1st place."
  [g slot-number turn-number place]
  (cond
    (>= turn-number (final-turn g)) (if (= slot-number
                                           (get (final-placements g) place))
                                      1.0 0.0)
    (<= turn-number 5) 1.0
    :else (get-in g [:planets turn-number slot-number])))

(defn expected-value-by-slot-on-turn
  "Map from player slot to expected value of final game result.
   Final result is 0 points for last place, +1 for each position above last place,
   and extra +1 for winning. Odds of each result estimated based on raw-tendency."
  [game turn]
  "Strategy:
     Start with most valuable result: 1st place.
     Each player's odds of getting that result considered proportional to raw-tendency.
     Next place, 2nd, each player's odds again proportional to raw-tendency, but
     also multiplied by 'remaining odds' 100% minus 1st place odds. Continue recursively
     down to last place.")
