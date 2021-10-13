import { Editor } from './editor';
import { File } from './file';

export type Config = {
  apiUrl: string;
  file: File;
  editor: Editor;
};
