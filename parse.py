#!/usr/bin/env python

import os, sys, re
from collections import defaultdict

PROP_RE = re.compile(r'^.*Found property: ([^ ]+) = (.*)$')
CONTEXT_RE = re.compile(r'^.*in context: \[([^]]+)\]$')


class Context(object):
    def __init__(self):
        self.children = {}
        self.dots = set()
        self.properties = defaultdict(lambda: set())

    def get_int(self, child, expand):
        tag = []
        dots = set()
        for sub in child.split('/'):
            if '.' in sub:
                t, d = sub.split('.', 1)
                if t in expand:
                    t = t + '.' + d
                dots.add(d)
                tag.append(t)
            else:
                tag.append(sub)
        tag = '/'.join(tag)
        if tag in self.children:
            c = self.children[tag]
        else:
            c = Context()
            self.children[tag] = c

        if dots:
            c.dots = c.dots.union(dots)

        return c

    def get(self, path, expand):
        split = path.split(" > ", 1)
        if len(split) == 2:
            return self.get_int(split[0], expand).get(split[1], expand)
        else:
            return self.get_int(split[0], expand)

    def add(self, prop, value):
        self.properties[prop].add(value)


def parse(f, expand):
    cur_prop = None
    root = Context()
    for line in f:
        prop_match = PROP_RE.match(line)
        context_match = CONTEXT_RE.match(line)
        if prop_match:
            cur_prop = prop_match.groups()
        elif context_match:
            ctx = root.get(context_match.group(1), expand)
            ctx.add(cur_prop[0], cur_prop[1])
    return root

global_safe_num = 0

def make_safe_name(node):
    global global_safe_num
    global_safe_num += 1
    return re.sub(r'[^a-zA-Z_0-9]', '_', node) + '_{}'.format(global_safe_num)

def recurse_out(out, node, name):
    safe_name = make_safe_name(name)
    suffix = ".*"
    if len(node.dots) == 0:
        suffix = ""
    elif len(node.dots) == 1:
        suffix = "." + list(node.dots)[0]
    example = []
    for key, values in node.properties.iteritems():
        if len(values) == 1:
            example.append("{} = {}".format(key, list(values)[0]))
        else:
            example.append("{} = {}".format(key, " .. ".join(list(values)[:3])))
        if len(example) > 3:
            example.append("...")
            break
    if example:
        example = "\\n" + "\\n".join(example)
    else:
        example = ""
    out.write('  {} [label="{}{}{}", shape=box];\n'.format(safe_name, name, suffix, example))
    for child_name, child in node.children.iteritems():
        child_name = recurse_out(out, child, child_name)
        out.write('  {} -> {};\n'.format(safe_name, child_name))
    return safe_name


def report(args):
    root = parse(open(args.input), set(args.expand))
    with open(args.output, 'w') as out:
        out.write('digraph ccs {\n')
        recurse_out(out, root, 'root')
        out.write('}\n')

if __name__ == '__main__':
    import argparse
    ap = argparse.ArgumentParser()
    ap.add_argument('input')
    ap.add_argument('output')
    ap.add_argument('--expand', nargs='*', default=[])
    args = ap.parse_args()
    report(args)
