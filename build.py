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
import glob


def rm(file):
    if os.path.exists(file):
        try:
            os.remove(file)
            print("Removed: " + file)
        except:
            print("Error while deleting file : ", file)


def createZip(input, output):
    shutil.make_archive(output, 'zip', input)
    print("Created: " + output + ".zip")


for file in glob.glob('ui/magisk/src/main/assets/*-module*.zip'):
    rm(file)


createZip(input='ui/magisk/module/empty-module',
          output='ui/magisk/src/main/assets/empty-module')

createZip(input='ui/magisk/module/rro-module',
          output='ui/magisk/src/main/assets/rro-module-2')
