import { Injectable, Logger } from '@nestjs/common';
import { createCipheriv, createDecipheriv, pbkdf2 } from 'crypto';

@Injectable()
export class EncryptionService {
  private readonly logger = new Logger(EncryptionService.name);
  private secret_key: string;
  private iv: Buffer;

  constructor() {
    pbkdf2(
      process.env.SECURITY_ENCRYPTION_KEY,
      'salt',
      100000,
      64,
      'sha512',
      (_, derivedKey) => {
        this.secret_key = derivedKey.toString('hex').substr(0, 32);
        this.iv = Buffer.from(derivedKey).subarray(0, 16);
      },
    );
    this.logger.debug(`Encrypting service has been initialized`);
  }

  Encrypt = (text: string) => {
    this.logger.debug('Encrypting a new message');
    const cipher = createCipheriv('aes-256-ctr', this.secret_key, this.iv);
    const encrypted = Buffer.concat([cipher.update(text), cipher.final()]);

    return encrypted.toString('hex');
  };

  Decrypt = (hash: string) => {
    this.logger.debug(`Decypting: ${hash}`);
    const decipher = createDecipheriv(
      'aes-256-ctr',
      this.secret_key,
      Buffer.from(this.iv.toString('hex'), 'hex'),
    );

    const decrpyted = Buffer.concat([
      decipher.update(Buffer.from(hash, 'hex')),
      decipher.final(),
    ]);

    return decrpyted.toString();
  };
}
