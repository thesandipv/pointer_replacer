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
