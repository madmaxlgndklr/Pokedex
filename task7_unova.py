import json

with open('app/src/main/assets/trainers.json', 'r') as f:
    data = json.load(f)

unova = {
  "name": "Unova",
  "trainers": [
    {
      "id": "unova-cilan", "name": "Cilan", "title": "Gym Leader",
      "trainerClass": "GYM_LEADER", "typeSpecialty": "Grass",
      "rosters": [{ "label": "BW", "team": [
        { "pokemonId": 511, "level": 12, "moves": ["vine-whip","leer","taunt","fury-swipes"] },
        { "pokemonId": 43,  "level": 12, "moves": ["absorb","poisonpowder","sleep-powder","acid"] },
        { "pokemonId": 69,  "level": 12, "moves": ["vine-whip","sleep-powder","wrap","acid"] },
        { "pokemonId": 420, "level": 12, "moves": ["absorb","growl","helping-hand","magical-leaf"] },
        { "pokemonId": 191, "level": 12, "moves": ["absorb","growth","ingrain","grass-whistle"] },
        { "pokemonId": 511, "level": 14, "moves": ["vine-whip","leer","taunt","leech-seed"] }
      ]}]
    },
    {
      "id": "unova-chili", "name": "Chili", "title": "Gym Leader",
      "trainerClass": "GYM_LEADER", "typeSpecialty": "Fire",
      "rosters": [{ "label": "BW", "team": [
        { "pokemonId": 513, "level": 12, "moves": ["incinerate","leer","taunt","fury-swipes"] },
        { "pokemonId": 58,  "level": 12, "moves": ["ember","bite","leer","odor-sleuth"] },
        { "pokemonId": 77,  "level": 12, "moves": ["ember","tail-whip","growl","stomp"] },
        { "pokemonId": 37,  "level": 12, "moves": ["ember","tail-whip","growl","quick-attack"] },
        { "pokemonId": 126, "level": 12, "moves": ["ember","smog","leer","confuse-ray"] },
        { "pokemonId": 513, "level": 14, "moves": ["incinerate","leer","taunt","nasty-plot"] }
      ]}]
    },
    {
      "id": "unova-cress", "name": "Cress", "title": "Gym Leader",
      "trainerClass": "GYM_LEADER", "typeSpecialty": "Water",
      "rosters": [{ "label": "BW", "team": [
        { "pokemonId": 515, "level": 12, "moves": ["water-gun","leer","taunt","fury-swipes"] },
        { "pokemonId": 60,  "level": 12, "moves": ["bubble","hypnosis","water-sport","water-gun"] },
        { "pokemonId": 116, "level": 12, "moves": ["water-gun","smokescreen","leer","bubble"] },
        { "pokemonId": 72,  "level": 12, "moves": ["water-gun","constrict","poison-sting","wrap"] },
        { "pokemonId": 86,  "level": 12, "moves": ["headbutt","growl","water-gun","rest"] },
        { "pokemonId": 515, "level": 14, "moves": ["water-gun","leer","taunt","scald"] }
      ]}]
    },
    {
      "id": "unova-lenora", "name": "Lenora", "title": "Gym Leader",
      "trainerClass": "GYM_LEADER", "typeSpecialty": "Normal",
      "rosters": [{ "label": "BW", "team": [
        { "pokemonId": 504, "level": 18, "moves": ["tackle","leer","bite","hypnosis"] },
        { "pokemonId": 506, "level": 18, "moves": ["tackle","leer","bite","work-up"] },
        { "pokemonId": 39,  "level": 18, "moves": ["pound","sing","defense-curl","rollout"] },
        { "pokemonId": 234, "level": 19, "moves": ["tackle","growl","psybeam","confuse-ray"] },
        { "pokemonId": 241, "level": 19, "moves": ["tackle","stomp","milk-drink","rollout"] },
        { "pokemonId": 507, "level": 20, "moves": ["tackle","leer","bite","retaliate"] }
      ]}]
    },
    {
      "id": "unova-burgh", "name": "Burgh", "title": "Gym Leader",
      "trainerClass": "GYM_LEADER", "typeSpecialty": "Bug",
      "rosters": [{ "label": "BW", "team": [
        { "pokemonId": 540, "level": 21, "moves": ["string-shot","bug-bite","razor-leaf","protect"] },
        { "pokemonId": 557, "level": 21, "moves": ["rock-blast","smack-down","bug-bite","withdraw"] },
        { "pokemonId": 123, "level": 22, "moves": ["quick-attack","leer","x-scissor","slash"] },
        { "pokemonId": 214, "level": 22, "moves": ["tackle","horn-attack","bug-bite","leer"] },
        { "pokemonId": 165, "level": 21, "moves": ["comet-punch","string-shot","swift","bug-bite"] },
        { "pokemonId": 542, "level": 23, "moves": ["x-scissor","leaf-blade","swords-dance","slash"] }
      ]}]
    },
    {
      "id": "unova-elesa", "name": "Elesa", "title": "Gym Leader",
      "trainerClass": "GYM_LEADER", "typeSpecialty": "Electric",
      "rosters": [{ "label": "BW", "team": [
        { "pokemonId": 587, "level": 25, "moves": ["volt-switch","quick-attack","aerial-ace","spark"] },
        { "pokemonId": 587, "level": 25, "moves": ["volt-switch","quick-attack","aerial-ace","spark"] },
        { "pokemonId": 100, "level": 25, "moves": ["spark","rollout","sonicboom","thunder-wave"] },
        { "pokemonId": 81,  "level": 25, "moves": ["thundershock","thunder-wave","sonicboom","screech"] },
        { "pokemonId": 239, "level": 25, "moves": ["thunder-punch","quick-attack","leer","swift"] },
        { "pokemonId": 523, "level": 27, "moves": ["flame-charge","volt-switch","quick-attack","stomp"] }
      ]}]
    },
    {
      "id": "unova-clay", "name": "Clay", "title": "Gym Leader",
      "trainerClass": "GYM_LEADER", "typeSpecialty": "Ground",
      "rosters": [{ "label": "BW", "team": [
        { "pokemonId": 551, "level": 29, "moves": ["bite","sand-attack","swagger","mud-slap"] },
        { "pokemonId": 536, "level": 28, "moves": ["surf","muddy-water","mud-shot","mud-bomb"] },
        { "pokemonId": 551, "level": 29, "moves": ["bite","sand-attack","swagger","mud-slap"] },
        { "pokemonId": 231, "level": 29, "moves": ["stomp","growl","take-down","mud-slap"] },
        { "pokemonId": 339, "level": 29, "moves": ["mud-bomb","surf","water-gun","amnesia"] },
        { "pokemonId": 530, "level": 31, "moves": ["earthquake","x-scissor","slash","rock-slide"] }
      ]}]
    },
    {
      "id": "unova-skyla", "name": "Skyla", "title": "Gym Leader",
      "trainerClass": "GYM_LEADER", "typeSpecialty": "Flying",
      "rosters": [{ "label": "BW", "team": [
        { "pokemonId": 528, "level": 33, "moves": ["air-cutter","confusion","attract","heart-stamp"] },
        { "pokemonId": 333, "level": 33, "moves": ["sing","take-down","peck","growl"] },
        { "pokemonId": 278, "level": 33, "moves": ["water-gun","supersonic","wing-attack","mist"] },
        { "pokemonId": 279, "level": 34, "moves": ["surf","wing-attack","supersonic","protect"] },
        { "pokemonId": 169, "level": 33, "moves": ["confuse-ray","supersonic","wing-attack","air-cutter"] },
        { "pokemonId": 521, "level": 35, "moves": ["aerial-ace","air-slash","work-up","leer"] }
      ]}]
    },
    {
      "id": "unova-brycen", "name": "Brycen", "title": "Gym Leader",
      "trainerClass": "GYM_LEADER", "typeSpecialty": "Ice",
      "rosters": [{ "label": "BW", "team": [
        { "pokemonId": 582, "level": 37, "moves": ["icicle-spear","astonish","iron-defense","ice-shard"] },
        { "pokemonId": 583, "level": 37, "moves": ["blizzard","icicle-spear","astonish","iron-defense"] },
        { "pokemonId": 459, "level": 37, "moves": ["powder-snow","icy-wind","wood-hammer","ice-shard"] },
        { "pokemonId": 220, "level": 37, "moves": ["powder-snow","mud-bomb","take-down","ice-shard"] },
        { "pokemonId": 362, "level": 38, "moves": ["blizzard","ice-shard","hail","light-screen"] },
        { "pokemonId": 614, "level": 39, "moves": ["blizzard","bulk-up","slash","sheer-cold"] }
      ]}]
    },
    {
      "id": "unova-drayden", "name": "Drayden", "title": "Gym Leader",
      "trainerClass": "GYM_LEADER", "typeSpecialty": "Dragon",
      "rosters": [{ "label": "BW", "team": [
        { "pokemonId": 611, "level": 41, "moves": ["slash","dragon-rage","slash","work-up"] },
        { "pokemonId": 611, "level": 41, "moves": ["slash","dragon-rage","dual-chop","work-up"] },
        { "pokemonId": 147, "level": 42, "moves": ["dragon-rage","wrap","slam","thunder-wave"] },
        { "pokemonId": 148, "level": 43, "moves": ["dragon-rage","aqua-tail","slam","thunder-wave"] },
        { "pokemonId": 330, "level": 43, "moves": ["dragon-claw","earthquake","crunch","fly"] },
        { "pokemonId": 612, "level": 43, "moves": ["dragon-dance","dragon-claw","slash","dual-chop"] }
      ]}]
    },
    {
      "id": "unova-shauntal", "name": "Shauntal", "title": "Elite Four",
      "trainerClass": "ELITE_FOUR", "typeSpecialty": "Ghost",
      "rosters": [{ "label": "BW", "team": [
        { "pokemonId": 563, "level": 48, "moves": ["shadow-ball","will-o-wisp","calm-mind","hex"] },
        { "pokemonId": 593, "level": 48, "moves": ["shadow-ball","surf","will-o-wisp","hex"] },
        { "pokemonId": 429, "level": 50, "moves": ["shadow-ball","calm-mind","will-o-wisp","mystical-fire"] },
        { "pokemonId": 623, "level": 50, "moves": ["shadow-punch","earthquake","heavy-slam","bulk-up"] },
        { "pokemonId": 356, "level": 52, "moves": ["shadow-ball","will-o-wisp","calm-mind","confuse-ray"] },
        { "pokemonId": 609, "level": 52, "moves": ["shadow-ball","flame-charge","will-o-wisp","calm-mind"] }
      ]}]
    },
    {
      "id": "unova-marshal", "name": "Marshal", "title": "Elite Four",
      "trainerClass": "ELITE_FOUR", "typeSpecialty": "Fighting",
      "rosters": [{ "label": "BW", "team": [
        { "pokemonId": 538, "level": 48, "moves": ["bulk-up","storm-throw","payback","rock-slide"] },
        { "pokemonId": 539, "level": 48, "moves": ["bulk-up","karate-chop","quick-guard","rock-slide"] },
        { "pokemonId": 107, "level": 50, "moves": ["fire-punch","ice-punch","thunder-punch","mach-punch"] },
        { "pokemonId": 106, "level": 50, "moves": ["high-jump-kick","blaze-kick","close-combat","rapid-spin"] },
        { "pokemonId": 237, "level": 52, "moves": ["close-combat","stone-edge","sucker-punch","rapid-spin"] },
        { "pokemonId": 534, "level": 52, "moves": ["dynamic-punch","stone-edge","bulk-up","payback"] }
      ]}]
    },
    {
      "id": "unova-grimsley", "name": "Grimsley", "title": "Elite Four",
      "trainerClass": "ELITE_FOUR", "typeSpecialty": "Dark",
      "rosters": [{ "label": "BW", "team": [
        { "pokemonId": 510, "level": 48, "moves": ["night-slash","fake-out","attract","thunder-wave"] },
        { "pokemonId": 560, "level": 48, "moves": ["crunch","high-jump-kick","dragon-dance","shed-skin"] },
        { "pokemonId": 430, "level": 50, "moves": ["foul-play","sucker-punch","pursuit","night-slash"] },
        { "pokemonId": 625, "level": 52, "moves": ["night-slash","iron-head","swords-dance","sucker-punch"] },
        { "pokemonId": 229, "level": 50, "moves": ["crunch","flamethrower","nasty-plot","foul-play"] },
        { "pokemonId": 553, "level": 52, "moves": ["crunch","earthquake","dragon-claw","outrage"] }
      ]}]
    },
    {
      "id": "unova-caitlin", "name": "Caitlin", "title": "Elite Four",
      "trainerClass": "ELITE_FOUR", "typeSpecialty": "Psychic",
      "rosters": [{ "label": "BW", "team": [
        { "pokemonId": 518, "level": 48, "moves": ["psychic","hypnosis","dream-eater","calm-mind"] },
        { "pokemonId": 576, "level": 48, "moves": ["psychic","calm-mind","thunderbolt","shadow-ball"] },
        { "pokemonId": 561, "level": 50, "moves": ["psychic","air-slash","ice-beam","calm-mind"] },
        { "pokemonId": 282, "level": 50, "moves": ["psychic","calm-mind","shadow-ball","moonblast"] },
        { "pokemonId": 196, "level": 52, "moves": ["psychic","calm-mind","shadow-ball","morning-sun"] },
        { "pokemonId": 579, "level": 52, "moves": ["psychic","calm-mind","thunder","focus-blast"] }
      ]}]
    },
    {
      "id": "unova-alder", "name": "Alder", "title": "Champion",
      "trainerClass": "CHAMPION", "typeSpecialty": "Mixed",
      "rosters": [{ "label": "BW", "team": [
        { "pokemonId": 617, "level": 54, "moves": ["bug-buzz","rock-slide","substitute","baton-pass"] },
        { "pokemonId": 626, "level": 56, "moves": ["head-charge","megahorn","earthquake","stone-edge"] },
        { "pokemonId": 621, "level": 56, "moves": ["dragon-claw","crunch","thunder-punch","fire-punch"] },
        { "pokemonId": 584, "level": 56, "moves": ["blizzard","autotomize","ice-shard","freeze-dry"] },
        { "pokemonId": 589, "level": 56, "moves": ["x-scissor","iron-head","aerial-ace","swords-dance"] },
        { "pokemonId": 637, "level": 58, "moves": ["bug-buzz","fire-blast","quiver-dance","roost"] }
      ]}]
    },
    {
      "id": "unova-cheren", "name": "Cheren", "title": "Rival",
      "trainerClass": "RIVAL", "typeSpecialty": "Mixed",
      "rosters": [{ "label": "Final Battle (BW)", "team": [
        { "pokemonId": 504, "level": 50, "moves": ["hypnosis","bite","super-fang","retaliate"] },
        { "pokemonId": 510, "level": 51, "moves": ["night-slash","fake-out","attract","thunder-wave"] },
        { "pokemonId": 521, "level": 51, "moves": ["aerial-ace","air-slash","work-up","leer"] },
        { "pokemonId": 579, "level": 52, "moves": ["psychic","calm-mind","thunder","focus-blast"] },
        { "pokemonId": 530, "level": 52, "moves": ["earthquake","x-scissor","slash","rock-slide"] },
        { "pokemonId": 500, "level": 53, "moves": ["flare-blitz","head-smash","superpower","wild-charge"] }
      ]}]
    }
  ]
}

data["regions"].append(unova)

with open('app/src/main/assets/trainers.json', 'w') as f:
    json.dump(data, f, indent=2)
