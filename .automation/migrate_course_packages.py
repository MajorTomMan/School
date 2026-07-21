#!/usr/bin/env python3
from __future__ import annotations

import json
from pathlib import Path
from typing import Any

ROOT = Path('tools/course-content')
TEXT_STYLES = {
    'textbook_text': 'textbook',
    'explanation': 'explanation',
    'historical_note': 'history',
    'prompt': 'prompt',
    'caption': 'caption',
}
RENDERERS = {
    'number_line_lesson': 'number_line',
}


def typed_scene_data(renderer: str, params: dict[str, Any]) -> dict[str, Any]:
    result: dict[str, Any] = dict(params)
    if renderer == 'number_line_lesson':
        if 'signed' in result and isinstance(result['signed'], str):
            value = result['signed'].strip().lower()
            if value not in {'true', 'false'}:
                raise ValueError(f'invalid signed value: {result["signed"]}')
            result['signed'] = value == 'true'
        if 'initial' in result and isinstance(result['initial'], str):
            result['initial'] = float(result['initial'])
    if renderer == 'opposite_quantities' and isinstance(result.get('scenes'), str):
        result['scenes'] = [item.strip() for item in result['scenes'].split(',') if item.strip()]
    return result


def convert_block(block: dict[str, Any]) -> dict[str, Any]:
    kind = block['type']
    if kind in TEXT_STYLES:
        return {'type': 'text', 'style': TEXT_STYLES[kind], 'text': block['text']}
    if kind == 'summary':
        return {'type': 'list', 'items': block['items']}
    if kind == 'worked_example':
        result = {
            'type': 'example',
            'label': block.get('label', '例题'),
            'statement': block['statement'],
            'steps': block.get('steps', []),
        }
        if block.get('result') is not None:
            result['result'] = block['result']
        return result
    if kind == 'visualization':
        renderer = block['renderer']
        return {
            'type': 'scene',
            'template': RENDERERS.get(renderer, renderer),
            'data': typed_scene_data(renderer, block.get('params', {})),
        }
    if kind in {'heading', 'conclusion'}:
        return {'type': kind, 'text': block['text']}
    if kind == 'exercise':
        result = {'type': 'exercise', 'stem': block['stem']}
        for key in ('number', 'choices', 'hints'):
            if key in block and block[key] not in (None, [], ''):
                result[key] = block[key]
        return result
    if kind == 'formula':
        result = {'type': 'formula', 'expression': block['expression']}
        if block.get('conditions'):
            result['conditions'] = block['conditions']
        return result
    if kind == 'list':
        return {'type': 'list', 'items': block['items']}
    if kind in {'text', 'scene', 'example'}:
        return block
    raise ValueError(f'unsupported old block type: {kind}')


def convert_page(page: dict[str, Any]) -> dict[str, Any]:
    result: dict[str, Any] = {
        'id': page['id'],
        'title': page['title'],
    }
    if page.get('aliases'):
        result['aliases'] = page['aliases']
    result['sourcePage'] = page['sourcePage']
    if page.get('sourcePageEnd', page['sourcePage']) != page['sourcePage']:
        result['sourcePageEnd'] = page['sourcePageEnd']
    result['blocks'] = [convert_block(block) for block in page['blocks']]
    return result


def convert_section(section: dict[str, Any], chapter_id: str | None = None) -> dict[str, Any]:
    section_id = section['id']
    if chapter_id and section_id.startswith('section-'):
        section_id = f'{chapter_id}-{section_id}'
    result: dict[str, Any] = {
        'id': section_id,
        'title': section['title'],
    }
    if section.get('number'):
        result['number'] = section['number']
    if section.get('aliases'):
        result['aliases'] = section['aliases']
    else:
        result['aliases'] = [section['title']]
    result['pages'] = [convert_page(page) for page in section['pages']]
    return result


def convert_course(course: dict[str, Any]) -> dict[str, Any]:
    textbook = course['textbook']
    pdf = textbook['pdf']
    clean_textbook = {
        key: textbook[key]
        for key in ('id', 'title', 'publisher', 'edition', 'grade', 'semester', 'subject')
    }
    clean_textbook['pdf'] = {
        'path': pdf['path'],
        'pageCount': pdf['pageCount'],
        'pageIndexOffset': pdf.get('pageIndexOffset', 0),
    }
    chapters = []
    for chapter in course['chapters']:
        clean_chapter: dict[str, Any] = {
            'id': chapter['id'],
            'title': chapter['title'],
            'number': chapter['number'],
            'aliases': chapter['aliases'],
            'sections': [convert_section(section, chapter['id']) for section in chapter['sections']],
        }
        if chapter.get('review'):
            clean_chapter['review'] = convert_section(chapter['review'], chapter['id'])
        chapters.append(clean_chapter)
    return {'textbook': clean_textbook, 'chapters': chapters}


def write_json(path: Path, value: dict[str, Any]) -> None:
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')


def main() -> None:
    for path in sorted(ROOT.glob('pep-math-*/course.json')):
        write_json(path, convert_course(json.loads(path.read_text(encoding='utf-8'))))
        print('migrated', path)
    for path in sorted((ROOT / 'manual').glob('**/*.json')):
        old = json.loads(path.read_text(encoding='utf-8'))
        write_json(path, convert_section(old))
        print('migrated', path)


if __name__ == '__main__':
    main()
