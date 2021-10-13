import { Block, KnownBlock } from '@slack/types';

type FileInfo = {
  id: string;
  created: number;
  timestamp: number;
  name: string;
  title: string;
  filetype: string;
  user: string;
  permalink_public: string;
};

export function BuildFileBlocks(
  files: FileInfo[],
  jwt_payload: string,
): (Block | KnownBlock)[] {
  const blocks: (Block | KnownBlock)[] = [
    {
      type: 'divider',
    },
  ];

  files.forEach((file) => {
    blocks.push(
      {
        type: 'section',
        text: {
          type: 'mrkdwn',
          text: `File name: ${file.name}\nDocument type: ${file.filetype}\nCreated: ${file.created}`,
        },
        accessory: {
          type: 'image',
          image_url:
            'https://cdn.iconscout.com/icon/free/png-256/docx-file-14-504256.png',
          alt_text: file.name,
        },
      },
      {
        type: 'context',
        elements: [
          {
            type: 'mrkdwn',
            text: `<${
              process.env.GATEWAY_PROTOCOL +
              '://' +
              process.env.GATEWAY_HOST +
              ':' +
              process.env.GATEWAY_PORT
            }/editor?file=${
              file.id
            }&token=${jwt_payload} | Open in ONLYOFFICE>`,
          },
        ],
      },
      {
        type: 'divider',
      },
    );
  });

  return blocks;
}
