if [ -n "${NAME+x}" ]; then
  echo "PS1=\"\u@$NAME$INDEX$ \"" >> ~/.bashrc
fi
