import base64
import bs4
import os
import shutil
import sys
from repodata import REPODATA

def __main__():
    args = sys.argv
    n = len(args)

    # If the environment author wants to create an environment with requirements
    # e.g. python3 yuna.py <environment_filename> <requirement_filename>
    # Where environment_filename and requirement_filename have the .html and .txt extensions, 
    # respectively
    if n == 3:
        environmentFilename = args[1]
        requirementsFilename = args[2]
        buildEnvironment(environmentFilename, requirementsFilename)
    # If the environment author wants to create an envionrment without requirements
    # e.g. python3 yuna.py <notebook>.html
    elif n == 2:
        environmentFilename = args[1]
        buildEnvironment(environmentFilename)
    # The environment author has specified an invalid number of commandline arguments
    else:
        print("Incorrect number of commandline arguments")


def buildEnvironment(environmentFilename, requirementsFilename=None):
    print(f"Creating {environmentFilename}")
    environment = copyTemplate(environmentFilename)
    requirements = readRequirements(requirementsFilename)
    embedRequirements(environment, requirements)
    print(f"Created {environmentFilename}")


def copyTemplate(environmentFilename):
    if not os.path.exists("../out"):
        os.mkdir("../out")
    return shutil.copyfile("template.html", f"../out/{environmentFilename}")


def readRequirements(filename):
    lines = list()

    # If the environment author wants to create an environment with requirements
    if filename:
        with open(filename) as file:
            for line in file:
                lines.append(line.rstrip())

    return lines


def embedRequirements(environment, requirements):
    if not requirements:
        print("There are no requirements to embed")
    elif not validateRequirements(requirements):
        print("Unable to embed requirements")
    else:
        print("Embedding requirements")
        with open(environment, "r") as environmentFile:
            soup = bs4.BeautifulSoup(environmentFile, "html.parser")

        with open(environment, "w") as environmentFile:
            requirementsString = "["
            for i in range(len(requirements)):
                if i == len(requirements) - 1:
                    requirementsString += f"\"{requirements[i]}\""
                else:
                    requirementsString += f"\"{requirements[i]}\", "
            requirementsString += "]"
            requirementsScript = soup.find(id="requirements")
            requirementsScript.string = requirementsString
            environmentFile.write(str(soup))

        visited = set()
        for requirement in requirements:
            bfs(environment, requirement, visited)
        print("Embedded requirements")


def validateRequirements(requirements):
    for requirement in requirements:
        if requirement not in REPODATA["packages"]:
            return False
        
    return True


def bfs(environment, requirement, visited):
    queue = list()
    visited.add(requirement)
    queue.append(requirement)
    while queue:
        with open(environment, "r") as environmentFile:
            soup = bs4.BeautifulSoup(environmentFile, "html.parser")

        package = queue.pop(0)

        print(f"Embedding {package}")

        # embed current requirement
        filename = REPODATA["packages"][package]["file_name"]
        with open(f"../res/packages/{filename}", "rb") as packageFile:
            contents = packageFile.read()
            encodedBytes = base64.b64encode(contents)
            encodedString = encodedBytes.decode("ascii")
            div = soup.find(id=package)
            div.string = encodedString

        for dependency in REPODATA["packages"][package]["depends"]:
            if dependency not in visited:
                visited.add(dependency)
                queue.append(dependency)

        with open(environment, "w") as environmentFile:
            environmentFile.write(str(soup))

        print(f"Embedded {package}")
    
    
if __name__ == "__main__":
    __main__()
