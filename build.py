#  Copyright (C) 2016-2021 Sandip Vaghela
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#
#          http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.

import shutil
import os


def rm(file):
    try:
        os.remove(file)
        print(f'Removed {file}')
    except OSError as e:
        if e.errno != errno.ENOENT:
            raise

if os.path.exists('ui/magisk/src/main/assets/empty-module.zip'):
    rm('ui/magisk/src/main/assets/empty-module.zip')

if os.path.exists('ui/magisk/src/main/assets/rro-module.zip'):
    rm('ui/magisk/src/main/assets/rro-module.zip')

print('Creating empty-module.zip')
shutil.make_archive('ui/magisk/src/main/assets/empty-module', 'zip', 'ui/magisk/module/empty-module')
print('Created empty-module.zip')

# print('Creating rro-module.zip')
# shutil.make_archive('ui/magisk/src/main/assets/rro-module', 'zip', 'ui/magisk/module/rro-module')
# print('Created rro-module.zip')
