"""Check compiled manifest boundaries required by Core security backports."""
import re

PRIVATE_COMPONENTS = {
    'org.microg.gms.auth.AskPermissionActivity',
    'org.microg.gms.auth.AskPackageOverrideActivity',
    'org.microg.gms.auth.loginservice.GoogleLoginService',
    'org.microg.gms.auth.WorkAccountAuthenticatorService',
    'org.microg.gms.location.manager.LocationManagerService',
    'org.microg.gms.location.provider.NetworkLocationProviderService',
    'org.microg.gms.location.provider.FusedLocationProviderService',
    'org.microg.gms.location.provider.GeocodeProviderService',
}

def enforce_private_components(xmltree):
    components, current, depth = [], None, 0
    for line in xmltree.splitlines():
        element = re.match(r'(\s*)E: (\S+)', line)
        if element:
            indent = len(element[1])
            if current is not None and indent <= depth:
                components.append(current)
                current = None
            if element[2] in {'service', 'activity'}:
                current, depth = {}, indent
        if current is None or len(line) - len(line.lstrip()) != depth + 2:
            continue
        name = re.search(r':name\([^)]*\)="([^"]+)"', line)
        if name: current['name'] = name[1]
        exported = re.search(r':exported\([^)]*\)=(false|true|\(type 0x12\)0x[01])\b', line)
        if exported: current['private'] = exported[1] in {'false', '(type 0x12)0x0'}
    if current is not None: components.append(current)
    for name in sorted(PRIVATE_COMPONENTS):
        matches = [item for item in components if item.get('name') == name]
        if len(matches) != 1 or matches[0].get('private') is not True:
            raise ValueError('Core security boundary must be explicitly non-exported: ' + name)
    return len(PRIVATE_COMPONENTS)
