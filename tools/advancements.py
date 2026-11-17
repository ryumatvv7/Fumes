"""Generates the Fumes advancement tab, effect icons and their translations. Run: python tools/advancements.py"""
import json
import os
import sys

sys.path.insert(0, os.path.dirname(__file__))
RES = os.path.join(os.path.dirname(__file__), '..', 'src', 'main', 'resources')
ADV = os.path.join(RES, 'data', 'fumes', 'advancement')
LANG = os.path.join(RES, 'assets', 'fumes', 'lang')

# name: (parent, icon, frame, criterion, en title, en desc, ru title, ru desc, hidden)
CODE = {'code': {'trigger': 'minecraft:impossible'}}


def effect(eid):
    return {'got': {'trigger': 'minecraft:effects_changed', 'conditions': {'effects': {f'fumes:{eid}': {}}}}}


ADVS = {
    'root': (None, 'fumes:gas_mask', 'task', {'tick': {'trigger': 'minecraft:tick'}},
             'Fumes', 'The air underground is not always safe',
             'Подземные газы', 'Воздух под землёй не всегда безопасен', False),
    'gas_mask': ('root', 'fumes:gas_mask', 'task',
                 {'mask': {'trigger': 'minecraft:inventory_changed', 'conditions': {'items': [{'items': 'fumes:gas_mask'}]}}},
                 'Breathe Easy', 'Get a gas mask',
                 'Дыши ровно', 'Получить противогаз', False),
    'filter_works': ('gas_mask', 'minecraft:charcoal', 'task', CODE,
                     'The Filter Works', 'Walk through thick gas wearing a gas mask',
                     'Фильтр работает', 'Пройти сквозь плотный газ в противогазе', False),
    'asphyxia': ('root', 'minecraft:glass_bottle', 'task', effect('asphyxia'),
                 'Out of Air', 'Choke in methane or carbon dioxide',
                 'Нечем дышать', 'Задохнуться в метане или углекислом газе', False),
    'carbon_monoxide': ('asphyxia', 'minecraft:campfire', 'task', effect('carbon_monoxide_poisoning'),
                        'The Silent Killer', 'Get carbon monoxide poisoning. Build a chimney!',
                        'Тихий убийца', 'Отравиться угарным газом. Построй дымоход!', False),
    'hydrogen_sulfide': ('asphyxia', 'minecraft:egg', 'task', effect('hydrogen_sulfide_poisoning'),
                         'Rotten Eggs', 'Breathe hydrogen sulfide deep underground',
                         'Тухлые яйца', 'Надышаться сероводородом глубоко под землёй', False),
    'scalded': ('root', 'minecraft:water_bucket', 'task', effect('scalded'),
                'Steam Bath', 'Get scalded by steam from lava and water',
                'С лёгким паром!', 'Ошпариться паром от лавы и воды', False),
    'firedamp': ('root', 'minecraft:tnt', 'goal', CODE,
                 'Firedamp', 'Be near a gas cloud when it explodes',
                 'Рудничный газ', 'Оказаться рядом со взрывом газового облака', False),
}

EFFECTS = {
    'asphyxia': ('Asphyxia', 'Удушье'),
    'carbon_monoxide_poisoning': ('Carbon Monoxide Poisoning', 'Отравление угарным газом'),
    'hydrogen_sulfide_poisoning': ('Hydrogen Sulfide Poisoning', 'Отравление сероводородом'),
    'scalded': ('Scalded', 'Ожог паром'),
}


def main():
    os.makedirs(ADV, exist_ok=True)
    en = json.load(open(os.path.join(LANG, 'en_us.json'), encoding='utf-8'))
    ru = json.load(open(os.path.join(LANG, 'ru_ru.json'), encoding='utf-8'))
    for name, (parent, icon, frame, crit, et, ed, rt, rd, hidden) in ADVS.items():
        disp = {
            'icon': {'id': icon},
            'title': {'translate': f'advancements.fumes.{name}.title'},
            'description': {'translate': f'advancements.fumes.{name}.description'},
            'frame': frame,
            'show_toast': parent is not None,
            'announce_to_chat': parent is not None,
            'hidden': hidden,
        }
        if parent is None:
            disp['background'] = 'minecraft:block/deepslate'
        adv = {'display': disp, 'criteria': crit, 'requirements': [list(crit.keys())]}
        if parent:
            adv = {'parent': f'fumes:{parent}', **adv}
        json.dump(adv, open(os.path.join(ADV, name + '.json'), 'w', encoding='utf-8'), indent=2, ensure_ascii=False)
        en[f'advancements.fumes.{name}.title'] = et
        en[f'advancements.fumes.{name}.description'] = ed
        ru[f'advancements.fumes.{name}.title'] = rt
        ru[f'advancements.fumes.{name}.description'] = rd
    for eid, (e, r) in EFFECTS.items():
        en[f'effect.fumes.{eid}'] = e
        ru[f'effect.fumes.{eid}'] = r
    json.dump(en, open(os.path.join(LANG, 'en_us.json'), 'w', encoding='utf-8'), indent=2, ensure_ascii=False)
    json.dump(ru, open(os.path.join(LANG, 'ru_ru.json'), 'w', encoding='utf-8'), indent=2, ensure_ascii=False)
    icons()
    print('advancements, effect names and icons written')


def icons():
    from textures import png
    root = os.path.join(RES, 'assets', 'fumes', 'textures', 'mob_effect')
    T = (0, 0, 0, 0)
    # 18x18 pictograms: a cloud of the gas colour with a symbol
    specs = {
        'asphyxia': ((110, 130, 170), 'x'),
        'carbon_monoxide_poisoning': ((176, 58, 72), 'skull'),
        'hydrogen_sulfide_poisoning': ((200, 185, 60), 'skull'),
        'scalded': ((235, 235, 240), 'wave'),
    }
    for eid, (col, sym) in specs.items():
        px = [[T] * 18 for _ in range(18)]
        dark = tuple(int(c * 0.55) for c in col) + (255,)
        fill = col + (255,)
        # puffy cloud: three overlapping circles
        for cx, cy, r in ((6, 10, 4.5), (11, 8, 5.2), (13, 11, 4)):
            for y in range(18):
                for x in range(18):
                    d = ((x - cx) ** 2 + (y - cy) ** 2) ** 0.5
                    if d <= r:
                        px[y][x] = fill
        for y in range(18):  # outline
            for x in range(18):
                if px[y][x] == fill and any(
                        not (0 <= x + dx < 18 and 0 <= y + dy < 18) or px[y + dy][x + dx] == T
                        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                    px[y][x] = dark
        mark = (30, 30, 34, 255)
        if sym == 'x':
            for i in range(5):
                px[7 + i][7 + i] = mark
                px[7 + i][11 - i] = mark
        elif sym == 'skull':
            for x in range(7, 12):
                for y in range(6, 10):
                    px[y][x] = (245, 245, 240, 255)
            px[7][8] = mark
            px[7][10] = mark
            for x in (8, 10):
                px[10][x] = (245, 245, 240, 255)
        else:
            for x in range(5, 14):
                px[8 + (1 if (x // 2) % 2 else 0)][x] = (220, 90, 40, 255)
                px[11 + (1 if (x // 2) % 2 else 0)][x] = (220, 90, 40, 255)
        png(os.path.join(root, eid + '.png'), 18, 18, px)


main()
