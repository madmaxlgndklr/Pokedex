import json

with open('app/src/main/assets/trainers.json', 'r') as f:
    data = json.load(f)

kalos = {
  "name": "Kalos",
  "trainers": [
    {
      "id": "kalos-viola", "name": "Viola", "title": "Gym Leader",
      "trainerClass": "GYM_LEADER", "typeSpecialty": "Bug",
      "rosters": [{ "label": "XY", "team": [
        { "pokemonId": 283, "level": 10, "moves": ["bubble","quick-attack","water-sport","stun-spore"] },
        { "pokemonId": 165, "level": 10, "moves": ["string-shot","bug-bite","comet-punch","swift"] },
        { "pokemonId": 284, "level": 11, "moves": ["bubble","quick-attack","water-sport","stun-spore"] },
        { "pokemonId": 11,  "level": 11, "moves": ["tackle","string-shot","harden","bug-bite"] },
        { "pokemonId": 588, "level": 11, "moves": ["vice-grip","string-shot","bug-bite","bide"] },
        { "pokemonId": 666, "level": 12, "moves": ["tackle","string-shot","stun-spore","silver-wind"] }
      ]}]
    },
    {
      "id": "kalos-grant", "name": "Grant", "title": "Gym Leader",
      "trainerClass": "GYM_LEADER", "typeSpecialty": "Rock",
      "rosters": [{ "label": "XY", "team": [
        { "pokemonId": 95,  "level": 25, "moves": ["rock-throw","bind","screech","dragon-breath"] },
        { "pokemonId": 185, "level": 25, "moves": ["rock-throw","harden","block","mimic"] },
        { "pokemonId": 222, "level": 25, "moves": ["tackle","harden","water-gun","ancient-power"] },
        { "pokemonId": 138, "level": 25, "moves": ["water-gun","bite","ancient-power","withdraw"] },
        { "pokemonId": 140, "level": 25, "moves": ["scratch","harden","rock-blast","ancient-power"] },
        { "pokemonId": 698, "level": 28, "moves": ["icy-wind","ancient-power","rock-tomb","aurora-beam"] }
      ]}]
    },
    {
      "id": "kalos-korrina", "name": "Korrina", "title": "Gym Leader",
      "trainerClass": "GYM_LEADER", "typeSpecialty": "Fighting",
      "rosters": [{ "label": "XY", "team": [
        { "pokemonId": 619, "level": 29, "moves": ["hi-jump-kick","u-turn","acrobatics","drain-punch"] },
        { "pokemonId": 67,  "level": 28, "moves": ["karate-chop","bulk-up","leer","rock-smash"] },
        { "pokemonId": 307, "level": 28, "moves": ["force-palm","calm-mind","confusion","detect"] },
        { "pokemonId": 308, "level": 29, "moves": ["force-palm","calm-mind","detect","hi-jump-kick"] },
        { "pokemonId": 448, "level": 30, "moves": ["aura-sphere","quick-attack","metal-claw","bone-rush"] },
        { "pokemonId": 448, "level": 32, "moves": ["aura-sphere","close-combat","quick-attack","metal-claw"] }
      ]}]
    },
    {
      "id": "kalos-ramos", "name": "Ramos", "title": "Gym Leader",
      "trainerClass": "GYM_LEADER", "typeSpecialty": "Grass",
      "rosters": [{ "label": "XY", "team": [
        { "pokemonId": 187, "level": 30, "moves": ["poison-powder","sleep-powder","leech-seed","mega-drain"] },
        { "pokemonId": 188, "level": 31, "moves": ["poison-powder","sleep-powder","leech-seed","mega-drain"] },
        { "pokemonId": 114, "level": 30, "moves": ["bind","sleep-powder","mega-drain","ancient-power"] },
        { "pokemonId": 315, "level": 30, "moves": ["magical-leaf","leech-seed","stun-spore","petal-blizzard"] },
        { "pokemonId": 673, "level": 31, "moves": ["vine-whip","growl","synthesis","grass-knot"] },
        { "pokemonId": 189, "level": 34, "moves": ["petal-dance","sleep-powder","leech-seed","cotton-guard"] }
      ]}]
    },
    {
      "id": "kalos-clemont", "name": "Clemont", "title": "Gym Leader",
      "trainerClass": "GYM_LEADER", "typeSpecialty": "Electric",
      "rosters": [{ "label": "XY", "team": [
        { "pokemonId": 82,  "level": 35, "moves": ["thunderbolt","tri-attack","thunder-wave","screech"] },
        { "pokemonId": 587, "level": 35, "moves": ["volt-switch","aerial-ace","acrobatics","spark"] },
        { "pokemonId": 101, "level": 35, "moves": ["thunderbolt","explosion","thunder-wave","charge-beam"] },
        { "pokemonId": 239, "level": 35, "moves": ["thunder-punch","quick-attack","meditate","light-screen"] },
        { "pokemonId": 310, "level": 36, "moves": ["thunderbolt","charge-beam","quick-attack","roar"] },
        { "pokemonId": 695, "level": 37, "moves": ["thunderbolt","dragon-pulse","charge","agility"] }
      ]}]
    },
    {
      "id": "kalos-valerie", "name": "Valerie", "title": "Gym Leader",
      "trainerClass": "GYM_LEADER", "typeSpecialty": "Fairy",
      "rosters": [{ "label": "XY", "team": [
        { "pokemonId": 122, "level": 38, "moves": ["psychic","moonblast","light-screen","calm-mind"] },
        { "pokemonId": 303, "level": 38, "moves": ["play-rough","iron-head","fairy-wind","crunch"] },
        { "pokemonId": 176, "level": 39, "moves": ["moonblast","ancient-power","metronome","growl"] },
        { "pokemonId": 468, "level": 40, "moves": ["moonblast","air-slash","aura-sphere","ancient-power"] },
        { "pokemonId": 35,  "level": 39, "moves": ["moonblast","doubleslap","sing","minimize"] },
        { "pokemonId": 700, "level": 42, "moves": ["moonblast","psychic","calm-mind","wish"] }
      ]}]
    },
    {
      "id": "kalos-olympia", "name": "Olympia", "title": "Gym Leader",
      "trainerClass": "GYM_LEADER", "typeSpecialty": "Psychic",
      "rosters": [{ "label": "XY", "team": [
        { "pokemonId": 561, "level": 44, "moves": ["psychic","air-slash","cosmic-power","ice-beam"] },
        { "pokemonId": 677, "level": 44, "moves": ["psychic","calm-mind","disarming-voice","iron-tail"] },
        { "pokemonId": 199, "level": 44, "moves": ["psychic","surf","calm-mind","thunder-wave"] },
        { "pokemonId": 576, "level": 45, "moves": ["psychic","calm-mind","thunderbolt","shadow-ball"] },
        { "pokemonId": 282, "level": 45, "moves": ["psychic","moonblast","calm-mind","shadow-ball"] },
        { "pokemonId": 678, "level": 48, "moves": ["psychic","calm-mind","shadow-ball","moonblast"] }
      ]}]
    },
    {
      "id": "kalos-wulfric", "name": "Wulfric", "title": "Gym Leader",
      "trainerClass": "GYM_LEADER", "typeSpecialty": "Ice",
      "rosters": [{ "label": "XY", "team": [
        { "pokemonId": 362, "level": 56, "moves": ["blizzard","ice-shard","hail","light-screen"] },
        { "pokemonId": 614, "level": 55, "moves": ["blizzard","bulk-up","slash","sheer-cold"] },
        { "pokemonId": 615, "level": 55, "moves": ["blizzard","freeze-dry","rapid-spin","light-screen"] },
        { "pokemonId": 460, "level": 56, "moves": ["blizzard","wood-hammer","ice-punch","leech-seed"] },
        { "pokemonId": 91,  "level": 56, "moves": ["blizzard","spikes","explosion","ice-shard"] },
        { "pokemonId": 471, "level": 59, "moves": ["blizzard","ice-shard","shadow-ball","quick-attack"] }
      ]}]
    },
    {
      "id": "kalos-malva", "name": "Malva", "title": "Elite Four",
      "trainerClass": "ELITE_FOUR", "typeSpecialty": "Fire",
      "rosters": [{ "label": "XY", "team": [
        { "pokemonId": 668, "level": 63, "moves": ["flamethrower","noble-roar","hyper-voice","dark-pulse"] },
        { "pokemonId": 324, "level": 63, "moves": ["overheat","earth-power","stealth-rock","will-o-wisp"] },
        { "pokemonId": 663, "level": 65, "moves": ["flare-blitz","brave-bird","roost","swords-dance"] },
        { "pokemonId": 609, "level": 65, "moves": ["fire-blast","shadow-ball","will-o-wisp","calm-mind"] },
        { "pokemonId": 136, "level": 65, "moves": ["flamethrower","quick-attack","shadow-ball","will-o-wisp"] },
        { "pokemonId": 59,  "level": 65, "moves": ["flamethrower","extreme-speed","wild-charge","close-combat"] }
      ]}]
    },
    {
      "id": "kalos-siebold", "name": "Siebold", "title": "Elite Four",
      "trainerClass": "ELITE_FOUR", "typeSpecialty": "Water",
      "rosters": [{ "label": "XY", "team": [
        { "pokemonId": 340, "level": 63, "moves": ["surf","earthquake","amnesia","yawn"] },
        { "pokemonId": 91,  "level": 63, "moves": ["surf","blizzard","spikes","explosion"] },
        { "pokemonId": 693, "level": 65, "moves": ["dragon-pulse","surf","sludge-bomb","shadow-ball"] },
        { "pokemonId": 689, "level": 65, "moves": ["surf","rock-slide","cross-chop","stone-edge"] },
        { "pokemonId": 121, "level": 65, "moves": ["surf","thunderbolt","ice-beam","recover"] },
        { "pokemonId": 131, "level": 66, "moves": ["surf","blizzard","thunder","confuse-ray"] }
      ]}]
    },
    {
      "id": "kalos-wikstrom", "name": "Wikstrom", "title": "Elite Four",
      "trainerClass": "ELITE_FOUR", "typeSpecialty": "Steel",
      "rosters": [{ "label": "XY", "team": [
        { "pokemonId": 227, "level": 63, "moves": ["steel-wing","aerial-ace","spikes","iron-head"] },
        { "pokemonId": 227, "level": 63, "moves": ["iron-head","aerial-ace","spikes","brave-bird"] },
        { "pokemonId": 212, "level": 65, "moves": ["x-scissor","iron-head","swords-dance","bullet-punch"] },
        { "pokemonId": 306, "level": 65, "moves": ["iron-head","earthquake","rock-slide","heavy-slam"] },
        { "pokemonId": 208, "level": 65, "moves": ["iron-tail","earthquake","crunch","stone-edge"] },
        { "pokemonId": 681, "level": 66, "moves": ["shadow-ball","iron-head","swords-dance","king-shield"] }
      ]}]
    },
    {
      "id": "kalos-drasna", "name": "Drasna", "title": "Elite Four",
      "trainerClass": "ELITE_FOUR", "typeSpecialty": "Dragon",
      "rosters": [{ "label": "XY", "team": [
        { "pokemonId": 334, "level": 63, "moves": ["dragon-pulse","sky-attack","dragon-dance","roost"] },
        { "pokemonId": 691, "level": 65, "moves": ["dragon-pulse","surf","sludge-bomb","shadow-ball"] },
        { "pokemonId": 621, "level": 65, "moves": ["dragon-claw","crunch","thunder-punch","fire-punch"] },
        { "pokemonId": 715, "level": 65, "moves": ["dragon-pulse","air-slash","roost","boomburst"] },
        { "pokemonId": 330, "level": 65, "moves": ["dragon-claw","earthquake","crunch","fly"] },
        { "pokemonId": 149, "level": 66, "moves": ["outrage","thunder","fire-blast","extreme-speed"] }
      ]}]
    },
    {
      "id": "kalos-diantha", "name": "Diantha", "title": "Champion",
      "trainerClass": "CHAMPION", "typeSpecialty": "Mixed",
      "rosters": [{ "label": "XY", "team": [
        { "pokemonId": 701, "level": 64, "moves": ["high-jump-kick","aerial-ace","swords-dance","feather-dance"] },
        { "pokemonId": 697, "level": 65, "moves": ["head-smash","dragon-claw","fire-fang","crunch"] },
        { "pokemonId": 699, "level": 65, "moves": ["blizzard","ancient-power","aurora-beam","encore"] },
        { "pokemonId": 711, "level": 66, "moves": ["shadow-ball","trick-or-treat","leech-seed","phantom-force"] },
        { "pokemonId": 706, "level": 66, "moves": ["dragon-pulse","fire-blast","sludge-wave","focus-blast"] },
        { "pokemonId": 282, "level": 68, "moves": ["moonblast","psychic","shadow-ball","calm-mind"] }
      ]}]
    },
    {
      "id": "kalos-serena", "name": "Serena", "title": "Rival",
      "trainerClass": "RIVAL", "typeSpecialty": "Mixed",
      "rosters": [{ "label": "Final Battle (XY)", "team": [
        { "pokemonId": 279, "level": 62, "moves": ["surf","air-slash","protect","fly"] },
        { "pokemonId": 700, "level": 63, "moves": ["moonblast","psychic","calm-mind","wish"] },
        { "pokemonId": 663, "level": 63, "moves": ["flare-blitz","brave-bird","roost","swords-dance"] },
        { "pokemonId": 660, "level": 63, "moves": ["return","sucker-punch","swords-dance","quick-attack"] },
        { "pokemonId": 121, "level": 64, "moves": ["surf","thunderbolt","ice-beam","recover"] },
        { "pokemonId": 655, "level": 65, "moves": ["fire-blast","moonblast","psychic","calm-mind"] }
      ]}]
    }
  ]
}

data["regions"].append(kalos)

with open('app/src/main/assets/trainers.json', 'w') as f:
    json.dump(data, f, indent=2)
